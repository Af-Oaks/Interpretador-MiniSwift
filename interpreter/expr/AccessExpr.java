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
            
            List<DictItem> list_elements = (List<DictItem>)a.data;
            DictType type = (DictType)a.type;
            int p =0;
            Object posicao = index.expr().data;;
            String classtype = posicao.getClass().toString();
            if(classtype.equals("class java.lang.Character")){
            p = Character.valueOf((Character)posicao);
            }else{
            p = (int)posicao;}
            
            //TODO verificar condicional do if
            if(type.getCategory() == Category.Dict){
                //TODO Condição abaixo não está sendo aceita
                //DictItem conteudo = (DictItem)value.data;
                DictItem conteudo = new DictItem();
                //conteudo.value = value.data;
                //conteudo.key = p;
                //TODO O proprio DictItem já tem chave e conteúdo, pq passar novamente ambos para a lista?
                //Onde que o valor da variavel original vai ser alterado?
                list_elements.set(p, conteudo);
            }
            else{
                System.out.println("ERRO ACESSEXPR SETVALUE()1111!!!!!!!!!!!");
                throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
            }
        }
        else{
            System.out.println("ERRO ACESSEXPR SETVALUE()2222!!!!!!!!!!!");
            throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, a.type.toString());
        }

    }

}