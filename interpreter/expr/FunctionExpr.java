package interpreter.expr;

import interpreter.value.Value;

public class FunctionExpr extends Expr {

    public static enum FunctionOp {
        Count,
        Empty,
        Keys,
        Values,
        Append,
        Contains
    }

    private FunctionOp op;
    private Expr expr;
    private Expr args;

    public FunctionExpr(int line,FunctionOp op,Expr expr,Expr args){
        super(line);
        this.op = op;
        this.expr = expr;
        this.args = args;
    }

    @Override
    public Value expr(){
        Value values = null;
        switch (op) {
            case Count:
                
                break;
            case Empty:
                
                break;
            case Keys:
                
                break;
            case Values:
                
                break;
            case Append:
                
                break;
            case Contains:
                
                break;
            default:
                System.out.println("ERRO FUNCTION.OP NAO EXISTENTE NO FUNCTIONEXPR.EXP()");
                break;
        }

        return values;
    }



}