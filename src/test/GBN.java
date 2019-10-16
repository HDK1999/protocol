package test;

import java.util.Timer;
import java.util.Random;
import java.util.TimerTask;
import java.net.*;
import java.io.*;

class GBNSender extends Thread{
    public int window_size=7;       //窗口大小
    public int data_size=20;
    public int[] sign=new int[data_size];
    public int latest_ACK=-1;   //最后收到的ACK
    public int window_sign[];        //当前窗口
    public int time_out=0;      //超时标志
    public int sign_ACK;        //sign of ACK:true:0;false:1;
    public GBNTimers sender_timer=null;    //发送方的计时器

    public void Send(){
        window_sign=new int[window_size];   //allocate space to windows
        for(int i=0;i<window_size;i++)
            window_sign[i]=sign[i];    //initialize:put the first three signs into windows
    }

    public void Run(){
        System.out.println("Sender is sending...");
    }

    public void ReceiveACK(int ACK){
        System.out.println("Sender Has Received ACK "+(ACK+1)+"\n Justifying......");
        if(ACK!=latest_ACK+1){
            System.out.println("ACK is false.Sender will resend data(sign: "+(latest_ACK+1)+") now.");
            sign_ACK=1;
        }
        else{
            System.out.println("ACK is True.");
            latest_ACK=ACK;
            sign_ACK=0;
        }
    }

    public void Time(){
        time_out=0;     //初始化
        sender_timer=new GBNTimers();
        Timer limit=new Timer();
        limit.schedule(sender_timer, 0, 100);
    }
}

class GBNReceiver extends Thread{
    public int last_data;
    public GBNSender sender;

    public void Run(GBNSender s){
        sender=s;
        System.out.println("Receiver is receiving...");
    }

    void Receive(int data,GBNSender s){
        sender=s;
        last_data=s.latest_ACK;
        System.out.println("Receiver has received data(sign:"+data+")");
        if(data!=0){
            if(data==last_data+1){
                System.out.println("TRUE.Receiver has accpeted it and will send ACK.");
                last_data=data;
                SendACK(last_data);
            }
            else{
                System.out.println("FALSE.Receiver will destroy it and send last ACK.");
                SendACK(last_data);
            }
        }
        else{
            System.out.println("TRUE.Receiver has accpeted it and will send ACK.");
            last_data=data;
            SendACK(last_data);
        }       //The first time to receive
    }

    void SendACK(int ack){
        if(sender.sender_timer.limit<20){   //judge if out of time(2s)
            ack=last_data;
            sender.ReceiveACK(ack);
        }
        else{
            System.out.println("------TIMEOUT------\nSender will resend data(sign: "+(sender.latest_ACK+1)+") now.");
            sender.time_out=1;
        }
    }
}

class GBNTimers extends TimerTask{
    public int time_out;
    public int limit;
    public void run(){
        if(limit<20) limit++;
        else{
            time_out=-1;
            this.cancel();
        }
    }
    public GBNTimers(){
        time_out=0;
        limit=0;
    }
}

public class GBN extends Thread{
    static void Send_Delay(int x) throws InterruptedException{
        if(x==1){
            sleep(300);
            System.out.println("--Delay:300ms--");
        }
        else if(x==2){
            sleep(750);
            System.out.println("--Delay:750ms--");
        }
        else if(x==3){
            sleep(1200);
            System.out.println("--Delay:1200ms--");
        }
        else if(x==4){
            sleep(3000);
            System.out.println("--Delay:3000ms--");
        }
    }
    public static void main(String[] args) throws IOException,InterruptedException{

        GBNSender s=new GBNSender();
        GBNReceiver r=new GBNReceiver();
        for(int i=0;i<s.data_size;i++){
            s.sign[i]=i;
        }
        //RUN Sender and Receiver
        s.Run();s.Send();
        r.Run(s);
        //Delay
        sleep(1000);
        //Times of resend.
        int[] re_times=new int[s.sign.length];
        int ran[]=new int[s.sign.length];
        int randelay;
        for(int i=0;i<s.sign.length;i++){
            re_times[i]=0;     //Initialization
        }
        //
        int i=0;
        while(i<s.sign.length){
            System.out.println();
            s.window_sign[0]=s.latest_ACK+1;
            i=s.window_sign[0];
            for(int k=0;k<s.window_size;k++)
                s.window_sign[k]=s.window_sign[0]+k;
            System.out.println();

            System.out.println("========CURRENT SENDER WINDOW========");
            for(int p=0;p<s.window_size;p++){
                if(s.window_sign[p]<s.sign.length)
                    System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN:"+s.window_sign[p]+" ||");
                else
                    System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN: VACANT"+" ||");
            }
            System.out.println("=====================================\n");

            if(i>=s.sign.length) break;

            for(int count=0;count<s.window_size && count+i<s.sign.length;count++){
                System.out.println("Send data(sign:"+(i+count)+(")."));
            }
            for(int count=0;count<s.window_size && count+i<s.sign.length;count++){
                re_times[s.latest_ACK+1+count]++;
            }

            s.Time();

            for(int count=0;count<s.window_size && count+i<s.sign.length;count++){
                System.out.println();
                randelay=new Random().nextInt(5);
                Send_Delay(randelay);
                ran[count+i]=new Random().nextInt(3);
                if(ran[count+i]!=1) r.Receive(i+count,s); //set random value,simulate the process of package loss
                else System.out.println("data of Sign: "+(i+count)+" has lost.");
            }
        }
        System.out.println("Stastic of resend times:");
        for(int count=0;count<s.sign.length;count++)
            System.out.println("Sign: "+count+"   RESEND_TIME:"+re_times[count]);
        System.exit(0);
    }
}