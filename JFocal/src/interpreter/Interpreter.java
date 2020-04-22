package interpreter;

import calculations.Calculate;
import program.ProgramLines;
import util.Iterator;
import util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Interpreter {

    private final static String WELCOME = "JFocal, version 0.34, 20 Apr 2020";
    private final static String PROMT = "*";

    private final static String A = "A";
    private final static String ASK = "ASK";
    private final static String C = "C";
    private final static String COMMENT = "COMMENT";
    private final static String D = "D";
    private final static String DO = "DO";
    private final static String F = "F";
    private final static String FOR = "FOR";
    private final static String G = "G";
    private final static String GOTO = "GOTO";
    private final static String I = "I";
    private final static String IF = "IF";
    private final static String R = "R";
    private final static String RETURN = "RETURN";
    private final static String S = "S";
    private final static String SET = "SET";
    private final static String T = "T";
    private final static String TYPE = "TYPE";
    private final static String Q = "Q";
    private final static String QUIT = "QUIT";

    private final static String GO = "GO";
    private final static String E = "E";
    private final static String ERASE = "ERASE";
    private final static String L = "L";
    private final static String LIBRARY = "LIBRARY";
    private final static String W = "W";
    private final static String WRITE = "WRITE";

    private final static String COMMAND_NOT_RECOGNIZED = "Error: Command '%s' not recognized";
    private final static String NOT_ENOUGH_PARAMETERS = "Error: Not enough parameters command '%s'";
    private final static String OPERATION_NOT_RECOGNIZED = "Error: Operation '%s' not recognized";
    private final static String UNPAIRED_QUOTES = "Error: Unpaired quotes '%s'";
    private final static String NO_LINE_WITH_NUMBER = "Error: No line with number %s";
    private final static String UNKNOWN_CTRL_CHARACTER = "Error: Unknown control character '%s'";
    private final static String INVALID_NUMBER_FORMAT = "Error: Invalid number format '%s'";

    private static String formatNumber = "%8.4f";

    private Scanner scanner;
    private ProgramLines program;
    private Map<String, Float> variables;
    private Iterator<Float> iterator;
    private Float currentLine; // current line number in program mode
    private boolean quit;

    public Interpreter() {
        scanner = new Scanner(System.in);
        program = new ProgramLines();
        variables = new HashMap<>();
        quit = false;
    }

    public void run() {
        System.out.println(WELCOME);
        while (!quit) {
            currentLine = null;
            System.out.print(PROMT);
            String line = scanner.nextLine();
            if (line.length() > 0) {
                if (line.toUpperCase().startsWith(GO)) {
                    if (program.size() > 0) {
                        goProgram(null);
                    }
                } else {
                    processLine(line);
                }
            }
        }
    }

    private void goProgram(Float toLine) {
        iterator = new Iterator<>(program.keySet());
        if (toLine == null) {
            currentLine = iterator.next();
        } else {
            currentLine = toLine;
            iterator.set(currentLine);
        }
        do {
            String line = program.get(currentLine);
            if (line != null) {
                Float result = processLine(line);
                if (result == 0) {
                    currentLine = iterator.next();
                } else if (result < 0) {
                    currentLine = null;
                } else {
                    if (!iterator.set(result)) {
                        Util.printErrorMsg(NO_LINE_WITH_NUMBER, String.valueOf(result), currentLine);
                        currentLine = null;
                    } else {
                        currentLine = result;
                    }
                }
            } else {
                currentLine = null;
            }
        } while (currentLine != null);
        quit = false;
    }

    private float commandAsk(String line) {
        String[] parameters = line.substring(line.indexOf(' ') + 1).split(",");
        for (String parameter : parameters) {
            if (parameter.startsWith("\"")) {
                if (parameter.endsWith("\"")) {
                    System.out.print(parameter.substring(1, parameter.length() - 1));
                } else {
                    Util.printErrorMsg(UNPAIRED_QUOTES, parameter, currentLine);
                    return -1;
                }
            } else {
                System.out.print(":");
                String stringNumber = scanner.nextLine();
                try {
                    // TODO the procedure for converting letters to numbers
                    float number = Float.parseFloat(stringNumber);
                    variables.put(Util.shortenVariableName(parameter.trim().toUpperCase()), number);
                } catch (NumberFormatException e) {
                    Util.printErrorMsg(Calculate.INVALID_NUMBER_FORMAT, stringNumber, currentLine);
                    return -1;
                }
            }
        }
        return 0;
    }

    private float commandDo(String doLine) {
        if (doLine == null) {
            Util.printErrorMsg(NOT_ENOUGH_PARAMETERS, "D/DO", currentLine);
            return -1;
        } if (Util.isValidLineNumber(doLine)) {
            return processLine(program.get(Float.parseFloat(doLine)));
        } else if (Util.isValidGroupNumber(doLine)) {
            Float numGroup = Float.parseFloat(doLine);
            Float returnToLine = 0f;
            if (iterator != null) {
                returnToLine = iterator.get(); // save line for return
                currentLine = iterator.firstInGroup(numGroup);
            } else {
                iterator = new Iterator<>(program.keySet()); // start program mode
                currentLine = iterator.firstInGroup(numGroup);
            }
            if (currentLine == null) {
                Util.printErrorMsg(ProgramLines.NO_LINE_IN_GROUP, doLine, currentLine);
                return -1;
            }
            iterator.set(currentLine);
            do {
                float number = processLine(program.get(currentLine));
                if (number == 0) {
                    currentLine = iterator.next();
                } else if (number > 0) {
                    currentLine = number;
                } else {
                    return -1;
                }
            } while (numGroup == currentLine.intValue());
            return returnToLine;
        } else {
            Util.printErrorMsg(ProgramLines.BAD_LINE_NUMBER, doLine, currentLine);
            return -1;
        }
    }

    private float commandFor(String[] parts) {
        String[] first = parts[0].substring(parts[0].indexOf(' ') + 1).split("=");
        String countName = first[0].trim().toUpperCase();
        String[] paramStr = first[1].trim().split(",");
        int init = Integer.parseInt(paramStr[0]);
        int stop = Integer.parseInt(paramStr.length < 3? paramStr[1] : paramStr[2]);
        int step = paramStr.length < 3? 1 : Integer.parseInt(paramStr[1]);
        for (int i = init; i <= stop; i += step) {
            variables.put(Util.shortenVariableName(countName), (float)i);
            for (int j = 1; j < parts.length; j++) {
                float result = processLine(parts[j].trim());
                if (result == -1) {
                    return -1;
                }
            }
        }
        return 0;
    }

    private float commandGoto(String toLine) {
        if (toLine == null) {
            Util.printErrorMsg(NOT_ENOUGH_PARAMETERS, "G/GOTO", currentLine);
            return -1;
        }
        Float number;
        if (Util.isValidLineNumber(toLine)) {
            number = Float.parseFloat(toLine);
        } else {
            Util.printErrorMsg(ProgramLines.BAD_LINE_NUMBER, toLine, currentLine);
            return -1;
        }
        String line = program.get(number);
        if (line == null) {
            Util.printErrorMsg(NO_LINE_WITH_NUMBER, toLine, currentLine);
            return -1;
        }
        if (currentLine == null) { // command mode
            goProgram(number);
        }
        return number;
    }

    public float commandIf(String line) {
        String[] parts = line.split("[()]");
        Float condition = Calculate.calculate(parts[1].trim(), variables);
        if (condition == null) {
            return -1;
        }
        condition = Math.signum(condition);
        String[] lineNums = parts[2].trim().split(",");
        for (int i = 0; i < 3; i++) {
            if (condition == (i - 1)) { // condition -> [-1,0,1]
                if (lineNums.length < i + 1) {
                    return 0;
                }
                String toLine = lineNums[i].trim();
                if (toLine.length() == 0) {
                    return 0;
                }
                if (Util.isValidLineNumber(toLine)) {
                    return Float.parseFloat(toLine);
                } else {
                    Util.printErrorMsg(ProgramLines.BAD_LINE_NUMBER, toLine, currentLine);
                    return -1;
                }
            }
        }
        return 0;
    }

    private float commandSet(String line) {
        String[] parts = line.substring(line.indexOf(' ') + 1).split("=");
        if (parts.length < 2) {
            Util.printErrorMsg(NOT_ENOUGH_PARAMETERS, "SET", currentLine);
            return -1;
        }
        Float result = Calculate.calculate(parts[1].trim(), variables);
        if (result != null) {
            variables.put(Util.shortenVariableName(parts[0].trim().toUpperCase()), result);
        } else {
            Util.printErrorMsg(null, null, currentLine);
            return -1;
        }
        return 0;
    }

    private float commandType(String line) {
        String[] parameters = Util.splitString(line.substring(line.indexOf(' ') + 1), ',');
        for (String parameter : parameters) {
            String item = parameter.trim();
            if (item.startsWith("\"")) {
                if (item.endsWith("\"")) {
                    System.out.print(item.substring(1, item.length() - 1));
                } else {
                    Util.printErrorMsg(UNPAIRED_QUOTES, item, currentLine);
                    return -1;
                }
            } else if (item.startsWith("%")) {
                if (item.equals("%")) {
                    formatNumber = "%e";
                } else if (!Util.isValidFormatNumber(item)) {
                    Util.printErrorMsg(INVALID_NUMBER_FORMAT, item, currentLine);
                    return -1;
                } else {
                    formatNumber = item + 'f';
                }
            } else if ("!#:".indexOf(item.substring(0, 1)) > -1) {
                for (int i = 0; i < item.length(); i++) {
                    char c = item.charAt(i);
                    switch (c) {
                        case '!':
                            System.out.println();
                            break;
                        case '#':
                            System.out.print("\r");
                            break;
                        case ':':
                            System.out.print("\t");
                            break;
                        default:
                            Util.printErrorMsg(UNKNOWN_CTRL_CHARACTER, Character.toString(c), currentLine);
                            return -1;
                    }
                }
            } else if (item.equals("$")) {
                for (String name : variables.keySet()) {
                    System.out.printf("%s=" + formatNumber + "\n", name, variables.get(name));
                }
            } else {
                Float result = Calculate.calculate(parameter, variables);
                if (result != null) {
                    System.out.printf(formatNumber, result);
                } else {
                    Util.printErrorMsg(null, null, currentLine);
                    return -1; // error in expression
                }
            }
        }
        return 0;
    }

    private void commandErase(String parameter) {
        if (parameter == null) {
            variables.clear();
        } else if (parameter.toUpperCase().equals("ALL")) {
            program.erase();
        } else {
            program.erase(parameter);
        }
    }

    private float commandLibrary(String[] tokens) {
        if (tokens.length < 3) {
            Util.printErrorMsg(NOT_ENOUGH_PARAMETERS, tokens[0], currentLine);
            return -1;
        }
        String operation = tokens[1].toUpperCase();
        switch (operation) {
            case "C":
            case "CALL":
                program.call(tokens[2]); // read program from file
                break;
            case "S":
            case "SAVE":
                program.save(tokens[2]); // save program to file
                break;
            default:
                Util.printErrorMsg(OPERATION_NOT_RECOGNIZED, tokens[1], currentLine);
                return -1;
        }
        return 0;
    }

    private void commandWrite(String parameter) {
        if (parameter == null) {
            // ignore command without parameter
        } else if (parameter.toUpperCase().equals("ALL")) {
            program.write();
        } else {
            program.write(parameter);
        }
    }

    private float processLine(String line) {
        if (line == null) {
            return -1;
        }
        String[] tokens = line.split(" ");
        if (Util.isValidLineNumber(tokens[0])) {
            if (tokens.length > 1) {
                program.add(tokens[0], line);
            } else {
                program.erase(tokens[0]);
            }
        } else {
            String[] parts = Util.splitString(line, ';');
            for (String part : parts) {
                tokens = part.split(" ");
                String cmd = tokens[0].toUpperCase();
                switch (cmd) {
                    case A:
                    case ASK:
                        if (commandAsk(part) < 0) {
                            return -1;
                        }
                        break;
                    case C:
                    case COMMENT:
                        break;
                    case D:
                    case DO:
                        return commandDo(tokens.length < 2? null: tokens[1]);
                    case F:
                    case FOR:
                        return commandFor(parts);
                    case G:
                    case GOTO:
                        return commandGoto(tokens.length < 2? null: tokens[1]);
                    case I:
                    case IF:
                        return commandIf(part);
                    case R:
                    case RETURN:
                        return 100;
                    case S:
                    case SET:
                        if (commandSet(part) < 0) {
                            return -1;
                        }
                        break;
                    case T:
                    case TYPE:
                        if (commandType(part) < 0) {
                            return -1;
                        }
                        break;
                    case Q:
                    case QUIT:
                        quit = true;
                        return -1;
                    case E:
                    case ERASE:
                        commandErase(tokens.length < 2? null : tokens[1]);
                        break;
                    case L:
                    case LIBRARY:
                        return commandLibrary(tokens);
                    case W:
                    case WRITE:
                        commandWrite(tokens.length < 2? null : tokens[1]);
                        break;
                    default:
                        Util.printErrorMsg(COMMAND_NOT_RECOGNIZED, tokens[0], currentLine);
                        return -1;
                }
            }
        }
        return 0;
    }
}
