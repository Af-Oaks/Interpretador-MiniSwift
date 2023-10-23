package interpreter.expr;



import interpreter.type.Type;
import interpreter.type.primitive.BoolType;
import interpreter.type.primitive.CharType;
import interpreter.type.primitive.FloatType;
import interpreter.type.primitive.IntType;
import interpreter.type.primitive.StringType;
import interpreter.value.Value;

public class CastExpr extends Expr {


    public static enum CastOp {
        ToBoolOp,
        ToIntOp,
        ToFloatOp, 
        ToCharOp,
        ToStringOp
    }


    private CastOp op;
    private Expr expr;

    public CastExpr(int line, CastOp op, Expr expr) {
        super(line);
        this.op = op;
        this.expr = expr;
    }

    @Override
    public Value expr() {
        //TODO: Pronto/Verificar 

        Value valor = expr.expr();
        
        Type.Category cat = valor.type.getCategory();

        switch(op){

            case ToBoolOp:

                switch(cat){
                    case Bool:
                        if((boolean)valor.data == false) {
                            return new Value(BoolType.instance(), false);}
                        else{
                            return new Value(BoolType.instance(), true);}
                    case Char:
                        if((char)valor.data == '0'){
                            return new Value(BoolType.instance(), false);}
                        else{
                            return new Value(BoolType.instance(), true);}
                    case Dict, Array:
                        if(valor.data.equals(null)){
                            return new Value(BoolType.instance(), false);}
                        else{
                            return new Value(BoolType.instance(), true);}
                    case Int:
                        if((int)valor.data == 0){
                            return new Value(BoolType.instance(), false);}
                        else{
                            return new Value(BoolType.instance(), true);}
                    case Float:
                        if((float)valor.data == 0.0){
                            return new Value(BoolType.instance(), false);}
                        else{
                            return new Value(BoolType.instance(), true);}
                    default:
                        return new Value(BoolType.instance(), true);
                                    
                }
            
            case ToIntOp:      
                if(cat == Type.Category.Int || cat == Type.Category.Char || cat == Type.Category.Float){
                        if (cat ==Type.Category.Float){
                        Float conv = (float)valor.data;
                        conv = (float)valor.data;
                        return new Value(FloatType.instance(), (conv.intValue()));
                        }

                    if (cat ==Type.Category.Char){
                        //System.out.println("\n\n\n\n\nRecompilou ------------------------------------------------------------------------"); 
                        Character c = (char)valor.data;
                        Integer conv = (int)c;
                        return new Value(IntType.instance(), conv.intValue());
                        }            
                    if (cat ==Type.Category.Int){ 
                        //System.out.println("\n\n\n\n\nRecompilou2 ------------------------------------------------------------------------");
                        return new Value(IntType.instance(), valor.data);}}
                else{
                    return new Value(IntType.instance(), 0);}

                case ToFloatOp: 
                if(cat == Type.Category.Int || cat == Type.Category.Char || cat == Type.Category.Float){
                    
                    if (cat ==Type.Category.Int){
                        Integer conv = (int)valor.data;
                        conv = (Integer)valor.data;
                        return new Value(FloatType.instance(), (conv.floatValue()));
                        }

                    if (cat ==Type.Category.Char){
                        //System.out.println("\n\n\n\n\nRecompilou ------------------------------------------------------------------------"); 
                        Character c = (char)valor.data;
                        Integer conv = (int)c;
                        Float conv2 = conv.floatValue();
                        return new Value(FloatType.instance(), conv2);
                        }            
                    if (cat ==Type.Category.Float){ 
                       // System.out.println("\n\n\n\n\nRecompilou ------------------------------------------------------------------------");
                        return new Value(FloatType.instance(), valor.data);}

                    //return new Value(FloatType.instance(), (conv));}
              
                    return new Value(FloatType.instance(), 0.0);}

            case ToCharOp:
                if(cat == Type.Category.Int || cat == Type.Category.Char){
                        //System.out.println("Só testando se recompilou mesmo  2    -\n");
                        if (cat ==Type.Category.Int){
                        Integer conv = (int)valor.data;   
                        Character c = (char)conv.byteValue() ;
                        return new Value(CharType.instance(), (c));
                        }

                    return new Value(CharType.instance(), (char)valor.data);}
                else{
                    return new Value(CharType.instance(), '\0');}

            case ToStringOp:
                return new Value(StringType.instance(), valor.data.toString());

            default:
                System.out.println("Alguma coisa deu muito errado no casting");
            break;

        }

        return null;
    }
    
}
