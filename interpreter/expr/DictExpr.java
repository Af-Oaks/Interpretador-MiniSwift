package interpreter.expr;


import java.util.List;

import interpreter.type.composed.DictType;

import interpreter.value.Value;



public class DictExpr extends Expr {

    private DictType type;
    private List<DictItem> items;

    public DictExpr(int line,DictType type,List<DictItem> items) {
        super(line);
        this.type = type;
        this.items = items;
    }

    @Override
    public Value expr() {
        System.out.println("Tá chegando no dict type");
        Value v = new Value(DictType.instance(type.getKeyType(), type.getValueType()), items);
        return v;
        
    }
    
}
