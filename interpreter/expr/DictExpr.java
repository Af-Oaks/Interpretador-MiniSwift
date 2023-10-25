package interpreter.expr;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        //criar map para retornar o value
        Map<Expr,Expr> realMap = new HashMap<Expr,Expr>();

        for (DictItem dictItem : items) {
            realMap.put(dictItem.key, dictItem.value);
        }
        Value map = new Value(type,realMap);


        return map;
        
    }
    
}
