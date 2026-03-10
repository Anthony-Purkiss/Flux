package flux;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

class Interpreter implements Stmt.Visitor<Void>, Expr.Visitor<Double> {
    private final Map<String, River> rivers;
    private final Map<String, Dam> dams;
    private final Map<String, List<String>>inflows;
    private final List<String> outputs;
    private final List<Double> rainfallLevels; 

    private boolean rainfallSet = false;

    Interpreter() {
        //Intialize core data structures
        rivers = new HashMap<>();
        dams = new HashMap<>();
        inflows = new LinkedHashMap<>();
        outputs = new ArrayList<>();
        rainfallLevels = new ArrayList<>();

        //Default rainfall value (1.0 for one day)
        rainfallLevels.add(1.0);
    }
    
    void interpret(List<Stmt> statements) {
        for(Stmt stmt : statements) {
            execute(stmt);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void evaluate() {
        System.out.println("Evaluating river system...");
        for(int day=0; day < rainfallLevels.size(); day++) {
            double rain = rainfallLevels.get(day);
            System.out.println("Day " + (day+1) + " rainfall: " + rain);

            //Apply rainfall to rivers and dams
            for(River river : rivers.values()) {
                river.inflow = rain;
                river.outflow = rain;
            }

            for(Dam dam : dams.values()) {
                dam.inflow = rain; //local rainfall contributes to dam inflow
                dam.outflow = 0.0;
            }

            //Propogate flows downstream
            for(String to : inflows.keySet()) {
                List<String> ups = inflows.get(to);
                double totalInflow = 0.0;

                for(String up : ups) {
                    //Determine whether the upstream node is a river or a dam
                    if(rivers.containsKey(up)) {
                        totalInflow += rivers.get(up).outflow;
                    } else if(dams.containsKey(up)) {
                        totalInflow += dams.get(up).outflow;
                    }
                }

                //Assign inflow to downstream node
                if(rivers.containsKey(to)) {
                    River r = rivers.get(to);
                    r.inflow += totalInflow;
                    r.outflow = r.inflow;
                } else if(dams.containsKey(to)) {
                    Dam d = dams.get(to);
                    d.inflow += totalInflow;
                    applyDamAlgorithm(d);
                }
            }

            //Apply dam algorithm to any terminal dams
            for(Dam dam : dams.values()) {
                boolean isUpstream = false;
                for(List<String> ups : inflows.values()) {
                    if(ups.contains(dam.name)) {
                        isUpstream = true;
                        break;
                    }
                }
                if(!isUpstream) {
                    applyDamAlgorithm(dam);
                }
            }

            //After all inflows are propogated for the day
            System.out.println("---- Day " + (day+1) + " Results ----");
            for(String name : outputs) {
                if(rivers.containsKey(name)) {
                    River r = rivers.get(name);
                    System.out.println("Output River " + name + ": inflow=" + r.inflow + ", outflow=" + r.outflow);
                } else if(dams.containsKey(name)) {
                    Dam d = dams.get(name);
                    System.out.println("Output Dam " + name + ": inflow=" + d.inflow + ", outflow=" + d.outflow);
                } else {
                    System.err.println("Warning: Output target '" + name + "' no longer exists.");
                }
            }
            System.out.println();
        }
    }

    //Statement Methods

    @Override
    public Void visitRainfallStmt(Stmt.Rainfall stmt) {
        if(rainfallSet) {
            System.err.println("Error: multiple rainfall() statements are not allowed.");
            return null;
        }

        rainfallLevels.clear(); //remove any previous values
        rainfallLevels.addAll(stmt.values);
        rainfallSet = true;
        return null;
    }

    @Override
    public Void visitRiverDeclStmt(Stmt.RiverDecl stmt) {
        String name = stmt.name.lexeme;

        //Check if this river already exists to avoid duplicates
        if(rivers.containsKey(name)) {
            System.err.println("Error: River '" + name + "' already declared.");
            return null;
        }

        //Create a new River object
        River river = new River(name);
        rivers.put(name, river);

        return null;
    }

    @Override
    public Void visitDamDeclStmt(Stmt.DamDecl stmt) {
        String name = stmt.name.lexeme;

        if(dams.containsKey(name)) {
            System.err.println("Error: Dam '" + name + "' already declared.");
            return null;
        }

        Dam dam = new Dam(name, stmt.threshold, stmt.minFlowRate, stmt.maxFlowRate);
        dams.put(name, dam);
        return null;
    }

    @Override
    public Void visitLinkStmt(Stmt.Link stmt) {
        String from = stmt.from.lexeme;
        String to = stmt.to.lexeme;

        //Check both nodes already exist
        if(!rivers.containsKey(from) && !dams.containsKey(from)) {
            System.err.println("Error: Source '" + from + "' not declared as a river or dam.");
            return null;
        }

        if(!rivers.containsKey(to) && !dams.containsKey(to)) {
            System.err.println("Error: Source '" + to + "' not declared as a river or dam.");
            return null;
        }

        //Ensure a river only flows into one thing
        for(List<String> ups : inflows.values()) {
            if(ups.contains(from)) {
                System.err.println("Error: River '" + from + "' already flows into another node.");
                return null;
            }
        }

        //Record the link: add 'from' as inflow to 'to'
        inflows.computeIfAbsent(to, k -> new ArrayList<>()).add(from);

        return null;
    }

    @Override 
    public Void visitMergeStmt(Stmt.Merge stmt) {
        String to = stmt.name.lexeme;
        List<String> upstreams = extractIdentifiers(stmt.expression);

        //Ensure all upstream rivers/dams exist
        for(String up : upstreams) {
            if(!rivers.containsKey(up) && !dams.containsKey(up)) {
                System.err.println("Error: Undeclared river or dam '" + up + "' used in merge statement.");
                return null;
            }
        }

        //Ensure downstream node exists
        if(!rivers.containsKey(to) && !dams.containsKey(to)) {
            System.err.println("Error: Downstream node '" + to + "' not declared before merge.");
            return null;
        }

        //Ensure each upstream river/dam doesn't already flow somewhere else
        for(String up : upstreams) {
            for(List<String> inflowList : inflows.values()) {
                if(inflowList.contains(up)) {
                    System.err.println("Error: River or dam '" + up + "' already flows into another node.");
                    return null;
                }
            }
        }

        //Add all upstream rivers to inflows map for this downstream
        inflows.computeIfAbsent(to, k -> new ArrayList<>()).addAll(upstreams);

        return null;
    }

    @Override 
    public Void visitOutputStmt(Stmt.Output stmt) {
        String name = stmt.name.lexeme;

        //Check that river/dam has been declared
        if(!rivers.containsKey(name) && !dams.containsKey(name)) {
            System.err.println("Error: Cannot output undeclared river or dam '" + name + "'.");
            return null;
        }

        outputs.add(name);
        return null;
    }

    //Expression methods (unused) 

    @Override
    public Double visitBinaryExpr(Expr.Binary expr) {
        //In Flux 2.0, expressions are structural, not computational. Binary expressions (like r1 + r2) are used only to build 
        //connections between nodes, they do not evaluate to numeric values. This method is implemented only to satisfy the 
        //Expr.Visitor interface
        return 0.0;
    }

    @Override 
    public Double visitIdentifierExpr(Expr.Identifier expr) {
        //Identifiers (like r1, dam1) represent river or dam names, not numeric values. Their meaning is resolved structurally 
        //during merge processing, not via runtime evaluation. This method returns a dummy value to complete the visitor interface.
        return 0.0;
    }

    //Helper methods

    private List<String> extractIdentifiers(Expr expr) {
        List<String> names = new ArrayList<>();

        if(expr instanceof Expr.Identifier) {
            //Base case: just a single river name
            names.add(((Expr.Identifier)expr).name.lexeme);
        } else if(expr instanceof Expr.Binary) {
            //Recursive case: left and right sides
            Expr.Binary binary = (Expr.Binary) expr;
            names.addAll(extractIdentifiers(binary.left));
            names.addAll(extractIdentifiers(binary.right));
        }

        return names;
    }

    private void applyDamAlgorithm(Dam d) {
        if(d.inflow < d.threshold) {
            d.outflow = d.inflow*d.minFlowRate/100.0;
        } else {
            d.outflow = d.inflow*d.maxFlowRate/100.0;
        }
    }
}