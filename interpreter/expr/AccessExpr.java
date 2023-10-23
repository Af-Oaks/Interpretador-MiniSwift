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

    protected AccessExpr(int line, SetExpr base, Expr index) {
        super(line);
        this.base = base;
        this.index = index;
    }

    @Override
    public Value expr() {
        // TODO: verificar funcionamento
        Value a = base.expr();

        if(Category.Array == a.type.getCategory() || Category.String == a.type.getCategory() ){

            List<Value> list_elements = (List<Value>)a.data;
            int posicao = (int)index.expr().data;
            Value element = list_elements.get(posicao);
            return new Value(element.type ,element);
        }
        else if(Category.Dict == a.type.getCategory() ){

            List<DictItem> list_elements = (List<DictItem>)a.data;
            DictType type = (DictType)a.type;
            int posicao = (int)index.expr().data;
            DictItem element = list_elements.get(posicao);
            return new Value(type ,element);
        }
        else{
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

            List<DictItem> list_elements = (List<DictItem>)a.data;
            DictType type = (DictType)a.type;
            int posicao = (int)index.expr().data;

            if(value.type.getCategory() == Category.Dict){
                DictItem conteudo = (DictItem)value.data;
                list_elements.set(posicao, conteudo);
            }
            else{

            }
        }
        else{
            throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
        }

    }

}