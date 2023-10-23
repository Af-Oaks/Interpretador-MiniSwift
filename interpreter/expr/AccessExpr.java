package interpreter.expr;

import interpreter.type.Type;
import interpreter.type.composed.ArrayType;
import interpreter.value.Value;
import lexical.Token;

public class AccessExpr extends SetExpr {

    private SetExpr base;
    private Expr index;

    protected AccessExpr(int line, SetExpr base, Expr index) {
        super(line);
        this.base = base;
        this.index = index;
    }

    @Override
    public Value expr() {
        // TODO Acessar o array na posição indicada
        //Descobrir como determinar o tipo do array
        Value a = new Value(ArrayType.instance(Type.Category.Array), base);

        Object array = base.expr().data;
       // if(Type.Array == base.expr().type){
            
       // }
       // if(array.get)
      //  Value conteudo;                                                 

        return null;}
   // }

    public void setValue(Value value){

        
    //TODO implementar

    }

}