package interpreter.expr;


import interpreter.type.primitive.BoolType;
import interpreter.value.Value;

public class ConditionalExpr extends Expr{

    private Expr cond;
    private Expr condTure;
    private Expr condFalse;

    public ConditionalExpr(int line,Expr cond, Expr condtrue, Expr condfalse) {
        super(line);
        this.cond = cond;
        condTure = condtrue;
        condFalse = condfalse;
    }

    @Override
    public Value expr() {
        Value condition = new Value(BoolType.instance(), cond.expr().data);
        if (condition.data.toString() == "true"){
           Value topass =  condTure.expr();
           return new Value(topass.type, topass.data);
        }else{
            Value topass =  condFalse.expr();
           return new Value(topass.type, topass.data);
        }
    }
    
}
