package edu.buffalo.cse.cse486586.groupmessenger2;
public class Message {

    public String type ;
    public String msg ;
    public int p;
    public int PNum;
    public int sent_by;
    public int seen_by;
    public int msgId;
    public boolean deliverable;

    public Message() {
        this.type="";
        this.msg="";
        this.p=-1;
        this.PNum=-1;
        this.sent_by=-1;
        this.seen_by = 0;
        this.msgId=-1;
        this.deliverable = false;
    }

}