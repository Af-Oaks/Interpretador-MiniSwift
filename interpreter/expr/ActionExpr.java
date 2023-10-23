package interpreter.expr;

import java.util.Scanner;
import java.util.Random;
import error.InternalException;
import interpreter.type.primitive.StringType;
import interpreter.type.primitive.IntType;
import interpreter.value.Value;

public class ActionExpr extends Expr {
    
    public static enum Op {
        Read,
        Random
    }

    private static Scanner in = new Scanner(System.in);
    private static Random aleato = new Random();
    private Op op;

    public ActionExpr(int line, Op op) {
        super(line);
        this.op = op;
    }

    @Override
    public Value expr() {
        switch (op) {
            case Read:
                return new Value(StringType.instance(), in.nextLine().trim());
            case Random:
                return new Value(IntType.instance(),aleato.nextInt());
            default:
                throw new InternalException("Unreachable");
        }
    }

}
