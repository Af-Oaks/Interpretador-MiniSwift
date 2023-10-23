package interpreter.expr;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import interpreter.type.composed.ArrayType;

import interpreter.value.Value;

public class ArrayExpr extends Expr {

    private ArrayType type;
    private List<Expr> itens;

    public ArrayExpr(int line, ArrayType type, List<Expr> itens) {
        super(line);
        this.type = type;
        this.itens = itens;
        type.getCategory();
    }

    @Override
    public Value expr() {
      
        List<Value> lista = new ArrayList<Value>(); 
         
       Iterator<Expr> iterator = itens.iterator();
         
        while(iterator.hasNext()){
            lista.add(iterator.next().expr());
        }
       // return null;
        return new Value(ArrayType.instance(type), lista); 
    }
}