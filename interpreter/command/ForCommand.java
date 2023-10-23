package interpreter.command;

import java.util.List;
import error.LanguageException;
import interpreter.expr.Expr;
import interpreter.expr.Variable;
import interpreter.type.Type.Category;
import interpreter.value.Value;

public class ForCommand extends Command {

    private Expr expr;
    private Command cmds;
    private Variable var;

    public ForCommand(int line,Variable var,Expr expr, Command cmds) {
        super(line);
        this.expr = expr;
        this.cmds = cmds;
        this.var = var;
    }

    @Override
    public void execute() {

        Value array = expr.expr();
        List<?> lista;
        char[] strings;
        if(array.type.getCategory()== Category.Array){//quando for array
            lista = (List<?>)array.data ;//arraylist
            //System.out.println("-------------------- ARRARY------------------------");
            for (Object itera : lista) { //Pode ser array 
                var.setValue(new Value(var.getType(),itera));
                cmds.execute();
            }
        }
        else if(array.type.getCategory()== Category.String && (var.getType().getCategory() == Category.Char)){ //quando for string
           // System.out.println("-------------------- IS TRING------------------------");
            strings =((String)array.data).toCharArray();
           // System.out.println("-------------------- tyRING------------------------");
            //System.out.println("--------------------Recompilou de novo com novos prints");
            for(char ch : strings){
                var.setValue(new Value(var.getType(),ch));
                cmds.execute();
            }          
        }
        else{
            throw LanguageException.instance(super.getLine(), LanguageException.Error.InvalidType, array.type.toString());
        }

    }

}
