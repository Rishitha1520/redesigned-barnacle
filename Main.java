import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    //simple operator description
    static class Op
    {
        int prec; boolean right; char sym;
        Op(char s, int p, boolean r)
        {
            sym = s; prec = p; right = r;
        }
    }

    static Map<Character, Op> OPS = new HashMap<>();
    static {
        OPS.put('+', new Op('+', 1, false));
        OPS.put('-', new Op('-', 1, false));
        OPS.put('*', new Op('*', 2, false));
        OPS.put('/', new Op('/', 2, false));
        OPS.put('^', new Op('^', 3, true));
        OPS.put('=', new Op('=', 3, false));
        OPS.put(',', new Op(',', 3, false));
    }

    // variables and history
    static Map<String, Double> vars = new HashMap<>();
    static List<String> history = new ArrayList<>();

    // token
    enum TType { NUM, VAR, FUNC, OP, LP, RP, COMMA }
    static class Tok {
        TType type; String text;
        Tok(TType t, String s){ type=t; text=s; }
    }

    // tokenize
    static List<Tok> lex(String s) {
        List<Tok> out = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c) || c == '.') {
                int j = i+1;
                while (j<n && (Character.isDigit(s.charAt(j)) || s.charAt(j)=='.')) j++;
                out.add(new Tok(TType.NUM, s.substring(i,j)));
                i = j;
            } else if (Character.isLetter(c)) {
                int j = i+1;
                while (j<n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j)=='_')) j++;
                String name = s.substring(i,j);
                if (isFunc(name)) out.add(new Tok(TType.FUNC, name));
                else out.add(new Tok(TType.VAR, name));
                i = j;
            } else if (c=='('){ out.add(new Tok(TType.LP,"(")); i++; }
            else if (c==')'){ out.add(new Tok(TType.RP,")")); i++; }
            else if (c==','){ out.add(new Tok(TType.COMMA,",")); i++; }
            else if (OPS.containsKey(c)) {
                out.add(new Tok(TType.OP, String.valueOf(c))); i++;
            } else throw new RuntimeException("Bad char: "+c);
        }
        return out;
    }

    //  shunting yard: infix -> RPN
    static List<Tok> toRPN(List<Tok> in) {
        List<Tok> out = new ArrayList<>();
        Deque<Tok> st = new ArrayDeque<>();
        for (Tok t : in) {
            switch (t.type) {
                case NUM: case VAR: out.add(t); break;
                case FUNC: st.push(t); break;
                case COMMA:
                    while (!st.isEmpty() && st.peek().type != TType.LP)
                        out.add(st.pop());
                    if (st.isEmpty()) throw new RuntimeException("Misplaced comma");
                    break;
                case OP:
                    Op o1 = OPS.get(t.text.charAt(0));
                    while (!st.isEmpty() && st.peek().type==TType.OP) {
                        Op o2 = OPS.get(st.peek().text.charAt(0));
                        boolean c1 = !o1.right && o1.prec <= o2.prec;
                        boolean c2 =  o1.right && o1.prec <  o2.prec;
                        if (c1 || c2) out.add(st.pop()); else break;
                    }
                    st.push(t); break;
                case LP: st.push(t); break;
                case RP:
                    while (!st.isEmpty() && st.peek().type != TType.LP)
                        out.add(st.pop());
                    if (st.isEmpty()) throw new RuntimeException("Mismatched )");
                    st.pop(); // (
                    if (!st.isEmpty() && st.peek().type==TType.FUNC)
                        out.add(st.pop());
                    break;
            }
        }
        while (!st.isEmpty()) {
            if (st.peek().type==TType.LP || st.peek().type==TType.RP)
                throw new RuntimeException("Mismatched ()");
            out.add(st.pop());
        }
        return out;
    }

    // evaluate RPN
    static double evalRPN(List<Tok> rpn) {
        Deque<Double> st = new ArrayDeque<>();
        for (Tok t : rpn) {
            switch (t.type) {
                case NUM: st.push(Double.parseDouble(t.text)); break;
                case VAR:
                    if (!vars.containsKey(t.text))
                        throw new RuntimeException("Unknown var: "+t.text);
                    st.push(vars.get(t.text)); break;
                case OP:
                    if (st.size()<2) throw new RuntimeException("Not enough operands");
                    double b = st.pop(), a = st.pop();
                    char op = t.text.charAt(0);
                    if (op=='/' && b==0.0) throw new RuntimeException("Divide by zero");
                    st.push(applyOp(op,a,b)); break;
                case FUNC:
                    if (st.isEmpty()) throw new RuntimeException("No arg for func");
                    double x = st.pop();
                    st.push(applyFunc(t.text, x)); break;
                default: throw new RuntimeException("Bad token in RPN");
            }
        }
        if (st.size()!=1) throw new RuntimeException("Bad expression");
        return st.pop();
    }

    static double applyOp(char op,double a,double b){
        switch(op){
            case '+': return a+b;
            case '-': return a-b;
            case '*': return a*b;
            case '/': return a/b;
            case '^': return Math.pow(a,b);
        }
        throw new RuntimeException("Unknown op "+op);
    }

    // one‑argument functions to keep it simple
    static boolean isFunc(String name){
        return name.equals("sin") || name.equals("cos") ||
                name.equals("tan") || name.equals("sqrt")||
                name.equals("log")||name.equals("ln");
    }
    static double applyFunc(String f,double x){
        double rad = Math.toRadians(x);
        switch(f){
            case "sin": return Math.sin(rad);
            case "cos": return Math.cos(rad);
            case "tan": return Math.tan(rad);
            case "sqrt":return Math.sqrt(x);
            case "ln": return Math.log(x);
            case "log": return Math.log10(x);
        }
        throw new RuntimeException("Unknown func "+f);
    }

    // convenience
    static double evalExpr(String expr){
        return evalRPN(toRPN(lex(expr)));
    }

    // CLI
    public static void main(String[] args) {
        vars.put("pi", Math.PI);
        vars.put("e", Math.E);

        Scanner sc = new Scanner(System.in);
        System.out.println("Calculator: commands set, eval, vars, history, export, quit");
        for (;;) {
            System.out.print("> ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                if (line.equals("quit") || line.equals("exit")) break;
                else if (line.startsWith("set ")) {
                    String rest = line.substring(4).trim();
                    String[] assignments = rest.split(",");
                    for (String assign : assignments){
                        assign = assign.trim();
                        int eq = rest.indexOf('=');
                        if (eq<=0) throw new RuntimeException("Use: set x=5");
                        String name = rest.substring(0,eq).trim();
                        String expr = rest.substring(eq+1).trim();
                        double v = evalExpr(expr);
                        vars.put(name, v);
                        System.out.println(name+" = "+v);
                    }
                } else if (line.startsWith("eval ")) {
                    String expr = line.substring(5).trim();
                    double r = evalExpr(expr);
                    System.out.println(r);
                    history.add(expr+" = "+r);
                } else if (line.equals("vars")) {
                    for (var e : vars.entrySet())
                        System.out.println(e.getKey()+" = "+e.getValue());
                } else if (line.equals("history")) {
                    for (String h : history) System.out.println(h);
                } else if (line.startsWith("export ")) {
                    String file = line.substring(7).trim();
                    Files.write(Paths.get(file), history);
                    System.out.println("History saved to "+file);
                } else {
                    double r = evalExpr(line);
                    System.out.println(r);
                    history.add(line+" = "+r);
                }
            } catch (Exception ex) {
                System.out.println("Error: "+ex.getMessage());
            }
        }
        sc.close();
    }
}