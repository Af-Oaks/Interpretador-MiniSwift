package syntatic;

import static error.LanguageException.Error.InvalidLexeme;
import static error.LanguageException.Error.UnexpectedEOF;
import static error.LanguageException.Error.UnexpectedLexeme;

import java.util.ArrayList;
import java.util.List;

import error.InternalException;
import error.LanguageException;
import interpreter.Environment;
import interpreter.Interpreter;
import interpreter.command.AssignCommand;
import interpreter.command.BlocksCommand;
import interpreter.command.Command;
import interpreter.command.DumpCommand;
import interpreter.command.ForCommand;
import interpreter.command.IfCommand;
import interpreter.command.InitializeCommand;
import interpreter.command.PrintCommand;
import interpreter.command.WhileCommand;
import interpreter.expr.AccessExpr;
import interpreter.expr.ActionExpr;
import interpreter.expr.ArrayExpr;
import interpreter.expr.BinaryExpr;
import interpreter.expr.CastExpr;
import interpreter.expr.CastExpr.CastOp;
import interpreter.expr.FunctionExpr.FunctionOp;
import interpreter.expr.ConditionalExpr;
import interpreter.expr.ConstExpr;
import interpreter.expr.DictExpr;
import interpreter.expr.DictItem;
import interpreter.expr.Expr;
import interpreter.expr.FunctionExpr;
import interpreter.expr.SetExpr;
import interpreter.expr.UnaryExpr;
import interpreter.expr.Variable;
import interpreter.type.Type;
import interpreter.type.composed.ArrayType;
import interpreter.type.composed.ComposedType;
import interpreter.type.composed.DictType;
import interpreter.type.primitive.BoolType;
import interpreter.type.primitive.CharType;
import interpreter.type.primitive.FloatType;
import interpreter.type.primitive.IntType;
import interpreter.type.primitive.PrimitiveType;
import interpreter.type.primitive.StringType;
import interpreter.value.Value;
import lexical.LexicalAnalysis;
import lexical.Token;

public class SyntaticAnalysis {

    private LexicalAnalysis lex;
    private Token current;
    private Token previous;
    private Environment environment;

    public SyntaticAnalysis(LexicalAnalysis lex) {
        this.lex = lex;
        this.current = lex.nextToken();
        this.previous = null;
        this.environment = Interpreter.globals;
    }

    public Command process() {
        Command cmd = procCode();
        eat(Token.Type.END_OF_FILE);
        return cmd;
    }

    private void advance() {
        //System.out.println("Found " + current);
        previous = current;
        current = lex.nextToken();
    }

    private void eat(Token.Type type) {
        if (type == current.type) {
            advance();
        } else {
            System.out.println("Expected (..., " + type + ", ..., ...), found " + current);
            reportError();
        }
    }

    private boolean check(Token.Type ...types) {
        for (Token.Type type : types) {
            if (current.type == type)
                return true;
        }

        return false;
    }

    private boolean match(Token.Type ...types) {
        if (check(types)) {
            advance();
            return true;
        } else {
            return false;
        }
    }

    private void reportError() {
        int line = current.line;
        switch (current.type) {
            case INVALID_TOKEN:
                throw LanguageException.instance(line, InvalidLexeme, current.lexeme);
            case UNEXPECTED_EOF:
            case END_OF_FILE:
                throw LanguageException.instance(line, UnexpectedEOF);
            default:
                throw LanguageException.instance(line, UnexpectedLexeme, current.lexeme);
        }
    }

    // <code> ::= { <cmd> }
    private BlocksCommand procCode() {
        int line = current.line;
        List<Command> cmds = new ArrayList<Command>();

        while (check(Token.Type.OPEN_CUR,
                Token.Type.VAR, Token.Type.LET,
                Token.Type.PRINT, Token.Type.PRINTLN,
                Token.Type.DUMP, Token.Type.IF,
                Token.Type.WHILE, Token.Type.FOR,
                Token.Type.NOT, Token.Type.SUB,
                Token.Type.OPEN_PAR, Token.Type.FALSE,
                Token.Type.TRUE, Token.Type.INTEGER_LITERAL,
                Token.Type.FLOAT_LITERAL, Token.Type.CHAR_LITERAL,
                Token.Type.STRING_LITERAL, Token.Type.READ,
                Token.Type.RANDOM, Token.Type.TO_BOOL,
                Token.Type.TO_INT, Token.Type.TO_FLOAT,
                Token.Type.TO_CHAR, Token.Type.TO_STRING,
                Token.Type.ARRAY, Token.Type.DICT, Token.Type.NAME)) {
            Command cmd = procCmd();
            if (cmd != null)
                cmds.add(cmd);
        }

        BlocksCommand bcmd = new BlocksCommand(line, cmds);
        return bcmd;
    }

    // <cmd> ::= <block> | <decl> | <print> | <dump> | <if> | <while> | <for> | <assign>
    private Command procCmd() {
        Command cmd = null;
        if (check(Token.Type.OPEN_CUR)) {
            cmd = procBlock();
        } else if (check(Token.Type.VAR, Token.Type.LET)) {
            cmd = procDecl();
        } else if (check(Token.Type.PRINT, Token.Type.PRINTLN)) {
            cmd = procPrint();
        } else if (check(Token.Type.DUMP)) {
            cmd = procDump();
        } else if (check(Token.Type.IF)) {
            cmd = procIf();
        } else if (check(Token.Type.WHILE)) {
            cmd = procWhile();
        } else if (check(Token.Type.FOR)) {
           cmd = procFor();
        } else if (check(Token.Type.NOT, Token.Type.SUB,
                Token.Type.OPEN_PAR, Token.Type.FALSE,
                Token.Type.TRUE, Token.Type.INTEGER_LITERAL,
                Token.Type.FLOAT_LITERAL, Token.Type.CHAR_LITERAL,
                Token.Type.STRING_LITERAL, Token.Type.READ,
                Token.Type.RANDOM, Token.Type.TO_BOOL,
                Token.Type.TO_INT, Token.Type.TO_FLOAT,
                Token.Type.TO_CHAR, Token.Type.TO_STRING,
                Token.Type.ARRAY, Token.Type.DICT, Token.Type.NAME)) {
            cmd = procAssign();
        } else {
            reportError();
        }

        return cmd;
    }

    // <block> ::= '{' <code> '}'
    private BlocksCommand procBlock() {
        eat(Token.Type.OPEN_CUR);

        Environment old = environment;
        environment = new Environment(old);

        BlocksCommand bcmd;
        try {
            bcmd = procCode();
            eat(Token.Type.CLOSE_CUR);
        } finally {
            environment = old;
        }

        return bcmd;
    }

    // <decl> ::= <var> | <let>
    private Command procDecl() {
        Command cmd = null;
        if (check(Token.Type.VAR)) {
           cmd = procVar();
        } else if (check(Token.Type.LET)) {
            cmd = procLet();
        } else {
            reportError();
        }
        return cmd;
    }

    // <var> ::= var <name> ':' <type> [ '=' <expr> ] { ',' <name> ':' <type> [ '=' <expr> ] } [';']
    private BlocksCommand procVar() {
        eat(Token.Type.VAR);
        int bline = previous.line;
        Token name = procName();
        eat(Token.Type.COLON);
        int line = previous.line;

        Type type = procType();
        Expr expr = null;

        Variable v = this.environment.declare(name, type, false);
        List<Command> cmds = new ArrayList<Command>();
        InitializeCommand icmd = null;

        if (match(Token.Type.ASSIGN)) {
            expr = procExpr();
            icmd = new InitializeCommand(line, v, expr);
            cmds.add(icmd);
        }

        while (match(Token.Type.COMMA)) {
            name = procName();
            eat(Token.Type.COLON);
            type = procType();
            v = this.environment.declare(name, type, true);
            if (match(Token.Type.ASSIGN)) {
                line = previous.line;
                expr = procExpr();
                icmd = new InitializeCommand(line, v, expr);
                cmds.add(icmd);
            }
        }

        match(Token.Type.SEMICOLON);
        BlocksCommand bcmd = new BlocksCommand(bline, cmds);
        return bcmd;
    }

    // <let> ::= let <name> ':' <type> '=' <expr> { ',' <name> ':' <type> '=' <expr> } [';']
    private BlocksCommand procLet() {
        eat(Token.Type.LET);
        int bline = previous.line;

        Token name = procName();
        eat(Token.Type.COLON);
        Type type = procType();

        Variable v = this.environment.declare(name, type, true);

        eat(Token.Type.ASSIGN);
        int line = previous.line;
        Expr expr = procExpr();

        List<Command> cmds = new ArrayList<Command>();
        InitializeCommand icmd = new InitializeCommand(line, v, expr);
        cmds.add(icmd);
        
        while (match(Token.Type.COMMA)) {
            name = procName();
            eat(Token.Type.COLON);
            type = procType();
        
            v = this.environment.declare(name, type, true);
        
            eat(Token.Type.ASSIGN);

            expr = procExpr();
            line = previous.line;

            icmd = new InitializeCommand(line, v, expr);
            cmds.add(icmd);
        }
            
        match(Token.Type.SEMICOLON);

        BlocksCommand bcmd = new BlocksCommand(bline, cmds);
        return bcmd;
    }

    // <print> ::= (print | println) '(' <expr> ')' [';']
    private PrintCommand procPrint() {
        boolean newline = false;
        if (match(Token.Type.PRINT, Token.Type.PRINTLN)) {
            newline = (previous.type == Token.Type.PRINTLN);
        } else {
            reportError();
        }
        int line = previous.line;

        eat(Token.Type.OPEN_PAR);
        Expr expr = procExpr();
        eat(Token.Type.CLOSE_PAR);

        match(Token.Type.SEMICOLON);

        PrintCommand pcmd = new PrintCommand(line, expr, newline);
        return pcmd;
    }

    // <dump> ::= dump '(' <expr> ')' [';']
    private DumpCommand procDump() {
        eat(Token.Type.DUMP);
        int line = previous.line;
        eat(Token.Type.OPEN_PAR);
        Expr expr = procExpr();
        eat(Token.Type.CLOSE_PAR);
        match(Token.Type.SEMICOLON);

        DumpCommand dcmd = new DumpCommand(line, expr);
        return dcmd;
    }

    // <if> ::= if <expr> <cmd> [ else <cmd> ]
    private IfCommand procIf() {
        Expr expr;
        Command cmd, elsecmd = null;

        eat(Token.Type.IF);
        expr = procExpr();
        cmd = procCmd();
        if (match(Token.Type.ELSE)) {
        elsecmd = procCmd();
        }
        IfCommand ifcmd = new IfCommand(current.line, expr, cmd, elsecmd);

        return ifcmd;

    }

    // <while> ::= while <expr> <cmd>
    private WhileCommand procWhile() {
        eat(Token.Type.WHILE);
        int line = previous.line;

        Expr expr = procExpr();
        Command cmd = procCmd();

        WhileCommand wcmd = new WhileCommand(line, expr, cmd);
        return wcmd;
        
    }

    // <for> ::= for ( <name> | ( var | let ) <name> ':' <type> ) in <expr> <cmd>
    private ForCommand procFor() {
        System.out.println("Realmente recompilou e rodou o proc for novo");
        Environment old = environment;environment = new Environment(old);
        eat(Token.Type.FOR);
        Variable var;Token tok;Type tip;
        if( !(check(Token.Type.VAR, Token.Type.LET)) ){
            tok = procName();
        } else{
            if(check(Token.Type.VAR)){eat(Token.Type.VAR);}
            else{match(Token.Type.LET);}
            tok = procName();
            eat(Token.Type.COLON);
            tip = procType();  
            environment.declare(tok,tip,true);
        }
        eat(Token.Type.IN);
        var = environment.get(tok);// pega a variavel do enviroment
        Expr exsp = procExpr();
        Command cmd = procCmd();
        ForCommand forcomand = new ForCommand(current.line,var,exsp,cmd);
        return  forcomand;  
    }

    // <assign> ::= [ <expr> '=' ] <expr> [ ';' ]
    private AssignCommand procAssign() {
        int line = current.line;
        Expr rhs = procExpr();

        SetExpr lhs = null;
        if (match(Token.Type.ASSIGN)) {
            if (!(rhs instanceof SetExpr))
                throw LanguageException.instance(previous.line, LanguageException.Error.InvalidOperation);

            
            lhs = (SetExpr) rhs;
            rhs = procExpr();
        }

        match(Token.Type.SEMICOLON);

        AssignCommand acmd = new AssignCommand(line, rhs, lhs);
        return acmd;
    }

    // <type> ::= <primitive> | <composed>
    private Type procType() {
        if (check(Token.Type.BOOL, Token.Type.INT, Token.Type.FLOAT,
                Token.Type.CHAR, Token.Type.STRING)) {
            return procPrimitive();
        } else if (check(Token.Type.ARRAY, Token.Type.DICT)) {
            return procComposed();
        } else {
            reportError();
            return null;
        }
    }

    // <primitive> ::= Bool | Int | Float | Char | String
    private PrimitiveType procPrimitive() {
        if (match(Token.Type.BOOL, Token.Type.INT,
                Token.Type.FLOAT, Token.Type.CHAR, Token.Type.STRING)) {
            switch (previous.type) {
                case BOOL:
                    return BoolType.instance();
                case INT:
                    return IntType.instance();
                case FLOAT:
                    return FloatType.instance();
                case CHAR:
                    return CharType.instance();
                case STRING:
                    return StringType.instance();
                default:
                    reportError();
            }
            // Do nothing.
        } else {
            reportError();
        }

        return null;
    }

    // <composed> ::= <arraytype> | <dicttype>
    private ComposedType procComposed() {
        
        ComposedType type;

        if (check(Token.Type.ARRAY)) {
            type = procArrayType();
            return type;
        } else if (check(Token.Type.DICT)) {
            type  = procDictType();
            return type;
        } else {
            reportError();
        }

        return null;
    }

    // <arraytype> ::= Array '<' <type> '>'
    private ArrayType procArrayType() {

        Type tipo;
        eat(Token.Type.ARRAY);
        eat(Token.Type.LOWER_THAN);
        tipo = procType();
        eat(Token.Type.GREATER_THAN);
        return ArrayType.instance(tipo);

    } 

    // <dicttype> ::= Dict '<' <type> ',' <type> '>'
    private DictType procDictType() {
        eat(Token.Type.DICT);
        eat(Token.Type.LOWER_THAN);
        Type tp1 = procType();
        eat(Token.Type.COMMA);
        Type tp2 = procType();
        eat(Token.Type.GREATER_THAN);
            
        return DictType.instance(tp1,tp2);
        
    }

    // <expr> ::= <cond> [ '?' <expr> ':' <expr> ]
    private Expr procExpr() {
        Expr expr = procCond();

        if (match(Token.Type.TERNARY)) {
            int line = previous.line;
            
            Expr Ctrue = procExpr();
            eat(Token.Type.COLON);
            Expr Cfalse = procExpr();

            ConditionalExpr condExpr = new ConditionalExpr(line, expr, Ctrue, Cfalse);

            return condExpr;
        }

        return expr;
        
    }

    // <cond> ::= <rel> { ( '&&' | '||' ) <rel> }
    private Expr procCond() {
        Expr left = procRel();

        while (match(Token.Type.AND, Token.Type.OR)) {
           Expr right;
           BinaryExpr.Op op;

            switch (previous.type) {
                case AND:
                    op = BinaryExpr.Op.And;
                    break;
                case OR:
                    op = BinaryExpr.Op.Or;
                    break;
                default:
                    right = null;
                    op = null;
                    break;
            }

            int line = previous.line;
            right = procRel();
            System.out.println("com condição if == null ----------");
            if(op != null)
                left = new BinaryExpr(line,left,op,right);
           
        }
        return left;
    }

    // <rel> ::= <arith> [ ( '<' | '>' | '<=' | '>=' | '==' | '!=' ) <arith> ]
    private Expr procRel() {
        Expr left = procArith();

        if (match(Token.Type.LOWER_THAN, Token.Type.GREATER_THAN,
                Token.Type.LOWER_EQUAL, Token.Type.GREATER_EQUAL,
                Token.Type.EQUALS, Token.Type.NOT_EQUALS)) {
            int line = previous.line;

            BinaryExpr.Op op;
            switch (previous.type) {
                case LOWER_THAN:
                    op = BinaryExpr.Op.LowerThan;
                    break;
                case GREATER_THAN:
                    op = BinaryExpr.Op.GreaterThan;
                    break;
                case LOWER_EQUAL:
                    op = BinaryExpr.Op.LowerEqual;
                    break;
                case GREATER_EQUAL:
                    op = BinaryExpr.Op.GreaterEqual;
                    break;
                case EQUALS:
                    op = BinaryExpr.Op.Equal;
                    break;
                case NOT_EQUALS:
                    op = BinaryExpr.Op.NotEqual;
                    break;
                default:
                    throw new InternalError("Unreachable");
            }

            Expr right = procArith();
            left = new BinaryExpr(line, left, op, right);
        }
        
        return left;
    }

    // <arith> ::= <term> { ( '+' | '-' ) <term> }
    private Expr procArith() {
        Expr left = procTerm();
        while (match(Token.Type.ADD, Token.Type.SUB)) {
            int line = previous.line;
            
            BinaryExpr.Op op = previous.type == Token.Type.ADD ?
                BinaryExpr.Op.Add : BinaryExpr.Op.Sub;

            Expr right = procTerm();
            left = new BinaryExpr(line, left, op, right);
        }

        return left;
    }

    // <term> ::= <prefix> { ( '*' | '/' ) <prefix> }
    private Expr procTerm() {
        Expr expr = procPrefix();
        while(match(Token.Type.MUL, Token.Type.DIV)){
            procPrefix();
        }
        //TODO: consertar isso
        
        return expr;
    }

    // <prefix> ::= [ '!' | '-' ] <factor>
    private Expr procPrefix() {
        UnaryExpr.Op op = null;
        int line = -1;
        if (match(Token.Type.NOT, Token.Type.SUB)) {
            switch (previous.type) {
                case NOT:
                    op = UnaryExpr.Op.Not;
                    break;
                case SUB:
                    op = UnaryExpr.Op.Neg;
                    break;
                default:
                    reportError();
            }

            line = previous.line;
        }

        Expr expr = procFactor();

        if (op != null)
            expr = new UnaryExpr(line, expr, op);

        return expr;
    }

    // <factor> ::= ( '(' <expr> ')' | <rvalue> ) <function>
    private Expr procFactor() {
        Expr expr = null;
        if (check(Token.Type.OPEN_PAR)) {
            eat(Token.Type.OPEN_PAR);
            expr = procExpr();
            eat(Token.Type.CLOSE_PAR);
        } else {
            expr = procRValue();
        }
        procFunction();

        return expr;
    }

    // <rvalue> ::= <const> | <action> | <cast> | <array> | <dict> | <lvalue>
    private Expr procRValue() {
        Expr expr = null;
        if (check(Token.Type.FALSE, Token.Type.TRUE,
                Token.Type.INTEGER_LITERAL, Token.Type.FLOAT_LITERAL,
                Token.Type.CHAR_LITERAL, Token.Type.STRING_LITERAL)) {
            expr = procConst();
        } else if (check(Token.Type.READ, Token.Type.RANDOM)) {
            expr = procAction();
        } else if (check(Token.Type.TO_BOOL, Token.Type.TO_INT,
                Token.Type.TO_FLOAT, Token.Type.TO_CHAR, Token.Type.TO_STRING)) {
          expr = procCast();
        } else if (check(Token.Type.ARRAY)) {
          expr = procArray();
        } else if (check(Token.Type.DICT)) {
           expr = procDict();
        } else if (check(Token.Type.NAME)) {
            expr = procLValue();
        } else {
            reportError();
        }

        return expr;
    }

    // <const> ::= <bool> | <int> | <float> | <char> | <string>
    private ConstExpr procConst() {
        Value value = null;
        if (check(Token.Type.FALSE, Token.Type.TRUE)) {
            value = procBool();
        } else if (check(Token.Type.INTEGER_LITERAL)) {
            value = procInt();
        } else if (check(Token.Type.FLOAT_LITERAL)) {
            value = procFloat();
        } else if (check(Token.Type.CHAR_LITERAL)) {
            value = procChar();
        } else if (check(Token.Type.STRING_LITERAL)) {
            value = procString();
        } else {
            reportError();
        }

        ConstExpr cexpr = new ConstExpr(previous.line, value);
        return cexpr;
    }

    // <bool> ::= false | true
    private Value procBool() {
        Value value = null;
        if (match(Token.Type.FALSE, Token.Type.TRUE)) {
            switch (previous.type) {
                case FALSE:
                    value = new Value(BoolType.instance(), false);
                    break;
                case TRUE:
                    value = new Value(BoolType.instance(), true);
                    break;
                default:
                    reportError();
            }
        } else {
            reportError();
        }

        return value;
    }

    // <action> ::= ( read  | random ) '(' ')'
    private ActionExpr procAction() {
        ActionExpr.Op op = null;
        if (match(Token.Type.READ, Token.Type.RANDOM)) {
            switch (previous.type) {
                case READ:
                    op = ActionExpr.Op.Read;
                    break;
                case RANDOM:
                    op = ActionExpr.Op.Random;
                    break;
                default:
                    throw new InternalException("Unrecheable");
            }
        } else {
            reportError();
        }

        int line = previous.line;

        eat(Token.Type.OPEN_PAR);
        eat(Token.Type.CLOSE_PAR);

        ActionExpr aexpr = new ActionExpr(line, op);
        return aexpr;
    }

    // <cast> ::= ( toBool | toInt | toFloat | toChar | toString ) '(' <expr> ')'
    private CastExpr procCast() {
        Expr expr;
        match(Token.Type.TO_BOOL, Token.Type.TO_INT, Token.Type.TO_FLOAT, Token.Type.TO_CHAR, Token.Type.TO_STRING);
        CastOp castTo;
        switch(previous.type){
            case TO_BOOL:
                castTo = CastOp.ToBoolOp;
                break;    
            case TO_INT:
                castTo = CastOp.ToIntOp;
                break;
            case TO_FLOAT:
                castTo = CastOp.ToFloatOp;
                break;
            case TO_CHAR:
                castTo = CastOp.ToCharOp;
                break;
            case TO_STRING:
                castTo = CastOp.ToStringOp;
                break;
            default:
                castTo = null;
                System.out.println("Alguma coisa deu errado no procCast");
                break;
        }
        eat(Token.Type.OPEN_PAR);
        expr = procExpr();
        eat(Token.Type.CLOSE_PAR);
        return new CastExpr(current.line, castTo, expr);   
    }

    // <array> ::= <arraytype> '(' [ <expr> { ',' <expr> } ] ')'
    private ArrayExpr procArray() {
        ArrayType tipoArray =procArrayType();
        eat(Token.Type.OPEN_PAR);
        List<Expr> lista = new ArrayList<Expr>();
        if (!check(Token.Type.CLOSE_PAR)) {
            lista.add(procExpr());
            while (match(Token.Type.COMMA)) {
                lista.add(procExpr());
            }
        }
        eat(Token.Type.CLOSE_PAR);
        ArrayExpr expressao = new ArrayExpr(current.line,tipoArray,lista);
        return expressao;
    }

    // <dict> ::= <dictype> '(' [ <expr> ':' <expr> { ',' <expr> ':' <expr> } ] ')'
    private DictExpr procDict() {
        System.out.println("Tá chegando no proc dict versão recompilada");
        DictType comptype = procDictType();
        eat(Token.Type.OPEN_PAR);
        List<DictItem> items = new ArrayList<DictItem>();
        DictItem carry = new DictItem();
        Expr expr1,expr2;
        if(!(match(Token.Type.CLOSE_PAR))){
            expr1 = procExpr();
            eat(Token.Type.COLON);
            expr2 =procExpr();
            carry.key =expr1; carry.value =expr2;// pego os valores do dict
            items.add(carry);
            while(match(Token.Type.COMMA)){
                expr1 = procExpr();
                eat(Token.Type.COLON);
                expr2 =procExpr();
                items.add(carry);
            }
            System.out.println("COMEU O PARENTESE");
            eat(Token.Type.CLOSE_PAR);
        }
        DictExpr dicionario = new DictExpr(current.line,comptype,items);
        return dicionario;
    }

    // <lvalue> ::= <name> { '[' <expr> ']' }
    private SetExpr procLValue() {
        
        //TODO Se tiver os colchetes retornar uma accessExpr, se não, variavel
        System.out.println("---------------------------------VALUE MODIFICADO V2-----------------------------");
        Token name = procName();
        SetExpr sexpr = this.environment.get(name);
        Expr index = null;
        if(check(Token.Type.OPEN_BRA)){
        while (match(Token.Type.OPEN_BRA)) {
            index = procExpr();
            eat(Token.Type.CLOSE_BRA);
        }
        return new AccessExpr(current.line, sexpr, index);
    }
    return sexpr;    
        
    }

    // <function> ::= { '.' ( <fnoargs> | <fonearg> ) }
    private FunctionExpr procFunction() {
        FunctionExpr funcExp = null;
        //TODO: verificar se é possivel pegar os EXPR e os Func.op
        while(match(Token.Type.DOT)){
            if(check(Token.Type.APPEND, Token.Type.CONTAINS)){
               funcExp = new FunctionExpr(current.line, null, funcExp, procFOneArg(funcExp)); 
            } else {
                 if(check(Token.Type.COUNT, Token.Type.EMPTY, Token.Type.KEYS, Token.Type.VALUES)){
                    funcExp = new FunctionExpr(current.line, null, funcExp, procFNoArgs(funcExp));
                 }
            }
        }

        return funcExp;
    }

    // <fnoargs> ::= ( count | empty | keys | values ) '(' ')'
    private FunctionExpr procFNoArgs(Expr exp) {
        match(Token.Type.COUNT, Token.Type.EMPTY, Token.Type.KEYS, Token.Type.VALUES);
        Token tk = previous;
        FunctionOp op =null;
        eat(Token.Type.OPEN_PAR);
        eat(Token.Type.CLOSE_PAR);
        switch(tk.type){
            case COUNT:
                op = FunctionOp.Count;
                break;
            case EMPTY:
                op = FunctionOp.Empty;
                break;
            case KEYS:
                op = FunctionOp.Keys;
                break;
            case VALUES:
                op = FunctionOp.Values;
                break;
            default:
                System.out.println("Alguma coisa deu muito errado no procFnoArgs");
                break;
        }
        FunctionExpr funcExpr = new FunctionExpr(current.line, op, exp, null);
        return funcExpr;

    }

    // <fonearg> ::= ( append | contains ) '(' <expr> ')'
    private FunctionExpr procFOneArg(Expr exp) {
        FunctionExpr funcExpr;
        Expr expr;
        FunctionOp op=null;
        if(check(Token.Type.APPEND)){
            eat(Token.Type.APPEND);
            op =FunctionOp.Append;
        }
        else if(check(Token.Type.CONTAINS)){
            eat(Token.Type.CONTAINS);
            op =FunctionOp.Contains;
        }
        else{
            reportError();
        }
        
        eat(Token.Type.OPEN_PAR);
        expr = procExpr();
        eat(Token.Type.CLOSE_PAR);
        funcExpr = new FunctionExpr(current.line,op,exp,expr);

        return funcExpr;
    }

    private Token procName() {
        eat(Token.Type.NAME);
        return previous;
    }

    private Value procInt() {
        Value v = current.literal;
        eat(Token.Type.INTEGER_LITERAL);
        return v;
    }

    private Value procFloat() {
        Value v = current.literal;
        eat(Token.Type.FLOAT_LITERAL);
        return v;
    }

    private Value procChar() {
        Value v = current.literal;
        eat(Token.Type.CHAR_LITERAL);
        return v;
    }

    private Value procString() {
        Value v = current.literal;
        eat(Token.Type.STRING_LITERAL);
        return v;
    }

}
