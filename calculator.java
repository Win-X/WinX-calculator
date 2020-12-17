import javax.swing.*;


import java.awt.*;
import java.awt.event.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class calculator
{

    static void mygui()
    {
        content frame=new content("WinXのCalculator");  //创建窗口
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    
    
        frame.setSize(600, 400);    //窗口大小
    
        frame.setVisible(true);     //显示窗口
    
    }
    public static void main(String[] args) 
    {
        mygui(); 
    }


	
    // 表达式字符合法性校验正则模式，静态常量化可以降低每次使用都要编译地消耗
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("[0-9\\.+-/*^()!= ]+");
    
    // 运算符优先级map
    private static final Map<String, Integer> OPT_PRIORITY_MAP = new HashMap<String, Integer>() {
        private static final long serialVersionUID = 6968472606692771458L;
        {
            put("(", 0);
            put("+", 2);
            put("-", 2);
            put("*", 3);
            put("/", 3);
            put(")", 7);
            put("^", 14);
            put("!", 14);
            put("=", 20);
        }
    };
    
    
    /**
     * 输入加减乘除表达式字符串，返回计算结果
     * @param expression 表达式字符串
     * @return 返回计算结果
     */
    public static double executeExpression(String expression) {
    	// 非空校验
    	if (null == expression || "".equals(expression.trim())) {
    	    throw new IllegalArgumentException("表达式不能为空！");
    	}
    	
    	// 表达式字符合法性校验
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("表达式含有非法字符！");
        }
        
        Stack<String> optStack = new Stack<>(); // 运算符栈
        Stack<BigDecimal> numStack = new Stack<>(); // 数值栈，数值以BigDecimal存储计算，避免精度计算问题
        StringBuilder curNumBuilder = new StringBuilder(16); // 当前正在读取中的数值字符追加器
        
        // 逐个读取字符，并根据运算符判断参与何种计算
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c != ' ') { // 空白字符直接丢弃掉
                if ((c >= '0' && c <= '9') || c == '.') {
                    curNumBuilder.append(c); // 持续读取一个数值的各个字符
                } else {
                    if (curNumBuilder.length() > 0) {
                        // 如果追加器有值，说明之前读取的字符是数值，而且此时已经完整读取完一个数值
                        numStack.push(new BigDecimal(curNumBuilder.toString()));
                        curNumBuilder.delete(0, curNumBuilder.length());
                    }
                	
                    String curOpt = String.valueOf(c);
                    if (optStack.empty()) {
                        // 运算符栈栈顶为空则直接入栈
                        optStack.push(curOpt);
                    } else {
                        if (curOpt.equals("(")) {
                            // 当前运算符为左括号，直接入运算符栈
                            optStack.push(curOpt);
                        } else if (curOpt.equals(")")) {
                            // 当前运算符为右括号，触发括号内的字表达式进行计算
                            directCalc(optStack, numStack, true);
                        } else if (curOpt.equals("=")) {
                            // 当前运算符为等号，触发整个表达式剩余计算，并返回总的计算结果
                            directCalc(optStack, numStack, false);
                            return numStack.pop().doubleValue();
                        } else {
                            // 当前运算符为加减乘除之一，要与栈顶运算符比较，判断是否要进行一次二元计算
                            compareAndCalc(optStack, numStack, curOpt);
                        }
                    }
                }
            }
        }
 
        // 表达式不是以等号结尾的场景
        if (curNumBuilder.length() > 0) {
    	    // 如果追加器有值，说明之前读取的字符是数值，而且此时已经完整读取完一个数值
            numStack.push(new BigDecimal(curNumBuilder.toString()));
    	}
        directCalc(optStack, numStack, false);
        return numStack.pop().doubleValue();
    }
    
    
    public static void compareAndCalc(Stack<String> optStack, Stack<BigDecimal> numStack, 
    		String curOpt) {
        // 比较当前运算符和栈顶运算符的优先级
        String peekOpt = optStack.peek();
        int priority = getPriority(peekOpt, curOpt);
        if (priority == -1 || priority == 0) {
            // 栈顶运算符优先级大或同级，触发一次二元运算
            String opt = optStack.pop(); // 当前参与计算运算符
            BigDecimal num2 = numStack.pop(); // 当前参与计算数值2
            BigDecimal num1 = numStack.pop(); // 当前参与计算数值1
            BigDecimal bigDecimal = floatingPointCalc(opt, num1, num2);
            
            // 计算结果当做操作数入栈
            numStack.push(bigDecimal);
            
            // 运算完栈顶还有运算符，则还需要再次触发一次比较判断是否需要再次二元计算
            if (optStack.empty()) {
                optStack.push(curOpt);
            } else {
                compareAndCalc(optStack, numStack, curOpt);
            }
        } else {
            // 当前运算符优先级高，则直接入栈
            optStack.push(curOpt);
        }
    }
    
  
    public static void directCalc(Stack<String> optStack, Stack<BigDecimal> numStack, 
    		boolean isBracket) {
                
        String opt = optStack.pop(); // 当前参与计算运算符
        if(opt=="!"){ BigDecimal tmp= numStack.pop().negate();   numStack.push(tmp);opt=optStack.pop();}
        BigDecimal num2 = numStack.pop(); // 当前参与计算数值2
        BigDecimal num1 = numStack.pop(); // 当前参与计算数值1
       
        BigDecimal bigDecimal = floatingPointCalc(opt, num1, num2);
        
        // 计算结果当做操作数入栈
        numStack.push(bigDecimal);
        
        if (isBracket) {
            if ("(".equals(optStack.peek())) {
                // 括号类型则遇左括号停止计算，同时将左括号从栈中移除
                optStack.pop();
            } else {
                directCalc(optStack, numStack, isBracket);
            }
        } else {
            if (!optStack.empty()) {
                // 等号类型只要栈中还有运算符就继续计算
                directCalc(optStack, numStack, isBracket);
            }
        }
    }
    
   
    public static BigDecimal floatingPointCalc(String opt, BigDecimal bigDecimal1, 
    BigDecimal bigDecimal2) {
        // double resultBigDecimal = 0;
        // double a=bigDecimal1.doubleValue();
        // double b=bigDecimal2.doubleValue();
        // switch (opt) {
        //     case "+":
        //         resultBigDecimal = a+b;
        //         break;
        //     case "-":
        //         resultBigDecimal = a-b;
        //         break;
        //     case "*":
        //         resultBigDecimal = a*b;
        //         break;
        //     case "/":
        //         resultBigDecimal = a/b; 
        //         break;
        //     case "^":
        //         resultBigDecimal = Math.pow(a,b);
        //     default:
        //         break;
        BigDecimal resultBigDecimal = new BigDecimal(0);
        switch (opt) {
            case "+":
                resultBigDecimal = bigDecimal1.add(bigDecimal2);
                break;
            case "-":
                resultBigDecimal = bigDecimal1.subtract(bigDecimal2);
                break;
            case "*":
                resultBigDecimal = bigDecimal1.multiply(bigDecimal2);
                break;
            case "/":
                resultBigDecimal = bigDecimal1.divide(bigDecimal2, 10, BigDecimal.ROUND_HALF_DOWN); // 注意此处用法
                break;
            case "%":
                resultBigDecimal = bigDecimal1.remainder(bigDecimal2);
            case "^":
                resultBigDecimal = bigDecimal1.pow(bigDecimal2.intValue());
            default:
                break;
        }
        return resultBigDecimal;
    }
    
   
    public static int getPriority(String opt1, String opt2) {
        int priority = OPT_PRIORITY_MAP.get(opt2) - OPT_PRIORITY_MAP.get(opt1);
        return priority;
    }
 

    
}


class content extends JFrame
{
        calculator c;
        JTextField tf,tf2;
        JTextArea History; 
        JScrollPane jslp;

        public content(String title)
        {
            super(title);
            ButtonListener listener =new ButtonListener();
            History=new JTextArea("");
            History.setPreferredSize(new Dimension(580, 110));
            History.setLineWrap(true);                          //自动换行
            History.setAlignmentX(LEFT_ALIGNMENT);
            History.setEditable(false);                                //历史记录框
            
        tf = new JTextField();
        tf2=new JTextField();
        tf.setPreferredSize(new Dimension(390, 30));
        tf.setHorizontalAlignment(JTextField.RIGHT);                    //整体表达式框
        tf2.setPreferredSize(new Dimension(150, 30));
        
        tf2.setHorizontalAlignment(JTextField.RIGHT);                   //当前操作字符框
        

        

        String btText[]= {"(",")","√","<-","sin","!","^","C","7","8","9","/","4","5","6","*","1","2","3","-","0",".","=","+"};
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(6,4));
        
        for(int i=0;i<24;i++)
        {
            JButton bt = new JButton(btText[i]);
            p.add(bt); 
            bt.setPreferredSize(new Dimension(70,30));
            bt.addActionListener(listener);
        }
        Container contentPane = getContentPane();
        
        contentPane.add(tf, BorderLayout.NORTH);
        this.add(tf2);  
        this.add(History,BorderLayout.SOUTH);                                             //添加组件
        contentPane.add(p,BorderLayout.CENTER);
        contentPane.setLayout(new FlowLayout());                    //新建组件
        }
    
    class ButtonListener implements ActionListener
        {
            String str="";
            String str2="";
            boolean str_bak=false;
            public void actionPerformed(ActionEvent e) 
            {
                String action = e.getActionCommand();
                char act=action.charAt(0);
                
                if(str_bak&&(act>='0'&&act<='9'))                                      //判断等号后如何操作
                {
                    str="";
                    str_bak=false;
                }
                else if(str_bak&&(act=='+'||act=='-'||act=='*'||act=='/'||act=='^'))        //等号后使用结果继续运算的一系列运算符
                {
                    str_bak=false;
                }

                if(act=='+'||act=='-'||act=='*'||act=='/'||act=='^'||act=='='||act=='!')              //对当前输入值进行操作
                {
                    str2="";
                    tf2.setText(str2);
                }
                else if(action=="sin")
                {
                    str=str.substring(str.length()-1,str2.length()-1);
                    str2=""+Math.sin(Double.parseDouble(str2));
                    str+=str2;
                    tf.setText(str);
                    tf2.setText(str2);
                }
                else if(action=="cos")
                {
                    str=str.substring(str.length()-1,str2.length()-1);
                    str2=""+Math.cos(Double.parseDouble(str2));
                    str+=str2;
                    tf.setText(str);
                    tf2.setText(str2);
                }
                else if(action=="<-")
                {
                    if(str2.length()==0){}
                    else
                    {
                    str2=str2.substring(0,str2.length()-1);
                    tf2.setText(str2);
                    }
                }
                else
                {
                    str2+=action;
                    tf2.setText(str2);
                }

                if(action=="=")                                             //对表达式框进行操作
                {
                    str_bak=true;
                History.setText(History.getText()+str);
                str= ""+c.executeExpression(str);
                tf.setText(str);
                History.setText(History.getText()+'='+str+ '\n' );
                
                }
                else if(action=="sin")
                {
                    
                }
                else if(action=="<-")
                {
                    if(str.length()==0){}
                    else
                    {
                    str=str.substring(0,str.length()-1);
                    tf.setText(str);
                    }
                }
                else if(action=="C")
                {
                    str="";
                    str2="";
                    str_bak=false;
                    tf.setText(str);
                    tf2.setText(str2);
                }
                else
                {
                    str +=action;
                    tf.setText(str);
                }

                
        
                
            }
        }
    }

    

