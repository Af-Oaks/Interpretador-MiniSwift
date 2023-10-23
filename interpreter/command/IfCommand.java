package interpreter.command;




import interpreter.expr.Expr;
import interpreter.type.primitive.BoolType;
import interpreter.value.Value;

public class IfCommand extends Command {

    private Expr expr;
    private Command thenCmds;
    private Command elseCommands;


    public IfCommand(int line, Expr expr, Command theCommand, Command elsCommand) {
        super(line);
        this.expr = expr;
        this.thenCmds = theCommand;
        this.elseCommands = elsCommand;
    }

    @Override
    public void execute() {
        Value value = expr.expr();
        BoolType boolType = BoolType.instance();
        if (boolType.match(value.type)) {
            boolean b = (Boolean) value.data;
        if(b){
        thenCmds.execute();
        
    }
    else{ if(elseCommands !=null) { elseCommands.execute();}}
    
    }

}
}
