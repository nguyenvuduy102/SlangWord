package SlangWord.io;

public class DataIssue {
    private  long lineNumber;
    private  String rawline;
    private  String reason;

    public  DataIssue(long lineNumber, String rawline, String reason) {
        this.lineNumber = lineNumber;
        this.rawline = rawline;
        this.reason = reason;
    }

    @Override 
    public String toString() {
        return lineNumber + "| " + rawline + " | " + reason;
    }
}

