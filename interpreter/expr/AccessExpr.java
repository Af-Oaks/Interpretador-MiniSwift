package interpreter.expr;

import java.util.List;
import java.util.Map;

import error.LanguageException;
import interpreter.type.Type.Category;
import interpreter.type.composed.DictType;
import interpreter.value.Value;

public class AccessExpr extends SetExpr {

    private SetExpr base;
    private Expr index;

    public AccessExpr(int line, SetExpr base, Expr index) {
        super(line);
        this.base = base;
        this.index = index;
    }

    @Override
    public Value expr() {
        // TODO: verificar funcionamento e adicionar lançamento de exceções
        Value a = base.expr();

        if(Category.Array == a.type.getCategory() || Category.String == a.type.getCategory() ){

            List<Value> list_elements = (List<Value>)a.data;
            
            int posicao = (int)index.expr().data;
            Value element = list_elements.get(posicao);
            return new Value(element.type ,element);
        }
        else if(Category.Dict == a.type.getCategory() ){

            Map<Expr,Expr> map = (Map<Expr,Expr>)a.data;
            DictType type = (DictType)a.type;
            Expr element = map.get(index);
            return new Value(type.getValueType() ,element);
        }
        else{
            System.out.println("ERRO ACESS EXPR - EXPR()!!!!!!!!!!!");
            throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
        }
                                                       
    }

    public void setValue(Value value){
        Value a = base.expr();

        if(Category.Array == a.type.getCategory() || Category.String == a.type.getCategory() ){

            List<Value> list_elements = (List<Value>)a.data;
            int posicao = (int)index.expr().data;
            list_elements.set(posicao, value);
            
        }
        else if(Category.Dict == a.type.getCategory() ){
            
            Map<Expr,Expr> map = (Map<Expr,Expr>)a.data;
            DictType tipo = (DictType)a.type;
            DictItem item = (DictItem)value.data;
        
           if(tipo.getCategory() == value.type.getCategory()){
                map.replace(item.key, item.value);
                base = (SetExpr)map;
           }
            else{
                System.out.println("ERRO SETVALUE!!!! VALUE.TYPE != DICT.TYPE");
                throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
            }
        }
        else{
            System.out.println("ERRO ACESSEXPR SETVALUE()NENHUM TIPO POSSIVEL ENCONTRADO!!!!!!!!!!!");
            throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
        }

    }

}