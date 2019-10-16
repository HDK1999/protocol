package test;
import java.util.Timer;
import java.util.Random;
import java.util.TimerTask;
import java.net.*;
import java.io.*;

class Sender extends Thread{
	public int window_size=7;       //窗口大小————S_w
	public int data_size=20;
	public int[] sign=new int[data_size];
	public int latest_ACK=-1;   //the latest ACK received
	public Timers sender_timer=null;    //发送方的定时器
	public int[] time_out=new int[window_size];      //超时标志
	public int window_sign[];        //current sign in windows

	public void Run(){
		window_sign=new int[window_size];   //allocate space to windows
		for(int i=0;i<window_size;i++){
			window_sign[i]=sign[i];    //initialize:put the first three signs into windows
			time_out[i]=0;
		}
		System.out.println("Sender is sending...");
	}

	public void ReceiveACK(int ACK){
		System.out.println("Sender Has Received ACK "+(ACK)+"\n Justifying......");
		//IF ACK NOT IN SEND_WINDOW
		if(ACK<latest_ACK+1 || ACK>latest_ACK+window_size){
			System.out.println("ACK is false.Sender will resend data(sign: "+(latest_ACK+1)+") now.");
		}
		else{
			System.out.println("ACK is True.");
			latest_ACK=ACK;
		}
	}

	public void ReceiveNAK(int NAK){
		latest_ACK=NAK;
		System.out.println("Sender Has Received NAK "+NAK+"\n Sender will resend data(sign:"+(latest_ACK+1)+") now.");
	}

	public void Time(int i){
		time_out[i]=0;     //Initialization
		sender_timer=new Timers();
		Timer limit=new Timer();
		limit.schedule(sender_timer, 0, 100);
	}
}

class Receiver extends Thread{
	public int window_size=7;
	public int window_sign[]=new int[window_size];        //current sign in windows
	public int last_data=-1;
	public Sender sender;
	public int data_size=20;
	public int[] sign=new int[data_size];
	int naksent=0;
	int received[]=new int[window_size];

	public void Run(Sender s){
		sender=s;
		window_sign=new int[window_size];   //allocate space to windows
		for(int i=0;i<window_size;i++){
			window_sign[i]=sender.sign[i];    //initialize:put the first three signs into windows
			received[i]=0;
		}
		System.out.println("Receiver is receiving...");
	}

	void Receive(int data,int timer_seq){
		Sender s=new Sender();
		s=sender;
		//last_data=s.latest_ACK;

		System.out.println("Receiver has received data(sign:"+data+")");
		for(int i=0;i<window_size;i++){
			if(received[i]==0){
				if(data==window_sign[i]){
					System.out.println("TRUE.Receiver has accpeted it.");
					received[i]=1;
					break;
				}
			}
		}
		//MOVE RECEIVE WINDOWS
		//FIND THE FIRST PLACE NOT RECIEVED:i.
		int i=0;
		for(i=0;i<window_size;i++){
			if(received[i]==0){
				break;
			}
		}
		//IF ALL WIN_DATA RECEIVED
		if(i==window_size){
			last_data=window_sign[i-1];
			window_sign[0]=window_sign[i-1]+1;
			for(int j=0;j<window_size;j++){
				received[j]=0;
			}
			for(int j=0;j<window_size-1;j++){
				window_sign[j+1]=window_sign[j]+1;
			}
			for(int j=0;j<window_size;j++) received[j]=0;
			//SEND ACK
			naksent=0;
			SendACK(last_data,timer_seq);
		}
		//IF NOT ALL RECEIVED
		else if(i!=0){  //IF i=0, NEED NOT MOVE
			last_data=window_sign[i-1];
			window_sign[0]=window_sign[i];
			for(int j=0;j<window_size;j++){
				if(j<window_size-i)
					received[j]=received[i+j];
				else received[j]=0;
				if(j<window_size-1) window_sign[j+1]=window_sign[j]+1;
				naksent=0;
				received[j]=0;
			}
			//SEND ACK
			SendACK(last_data,timer_seq);
		}
		//SEND NAK
		else{
			System.out.println("SEQUENCE FALSE.Receiver will send NAK of lastdata.");
			if(naksent==0){
				SendNAK(last_data,timer_seq);
				naksent=1;
			}
		}
	}

	void SendACK(int ack,int timer_seq){
		if(sender.sender_timer.limit<20){   //judge if out of time(2s)
			ack=last_data;
			sender.ReceiveACK(ack);
		}
		else{
			System.out.println("------TIMEOUT------\nSender will resend data(sign: "+(sender.latest_ACK+1)+") now.");
			sender.time_out[timer_seq]=1;
		}
	}

	void SendNAK(int nak,int timer_seq){
		if(sender.sender_timer.limit<20){   //judge if out of time(2s)
			nak=last_data;
			sender.ReceiveNAK(nak);
		}
		else{
			System.out.println("------TIMEOUT------\nSender will resend data(sign: "+(sender.latest_ACK+1)+") now.");
			sender.time_out[timer_seq]=1;
		}
	}
}

class Timers extends TimerTask{
	public int time_out;
	public int limit;
	public void run(){
		if(limit<20) limit++;
		else{
			time_out=-1;
			this.cancel();
		}
	}
	public Timers(){
		time_out=0;
		limit=0;
	}
}

public class SR_protocol extends Thread{
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

		Sender s=new Sender();
		Receiver r=new Receiver();
		for(int i=0;i<s.data_size;i++){     //Data to send/receive
			s.sign[i]=i;
			r.sign[i]=i;
		}
		//RUN Sender and Receiver
		s.Run();
		r.Run(s);
		//Delay
		sleep(1000);
		//Times of resend.
		int[] re_times=new int[s.sign.length];
		int ran;
		int randelay;
		for(int i=0;i<s.sign.length;i++){
			re_times[i]=0;     //Initialization
		}
		//
		int i=0;
		while(i<s.sign.length){
			//MOVE SENDER WINDOWS
			System.out.println();
			s.window_sign[0]=s.latest_ACK+1;
			i=s.window_sign[0];
			for(int k=0;k<s.window_size;k++){
				s.window_sign[k]=s.window_sign[0]+k;    //SET SEQ
				s.time_out[k]=0;    //RESET TIMER
			}
			System.out.println();
			//PRINT SENDER WINDOW
			System.out.println("========CURRENT SENDER WINDOW========");
			for(int p=0;p<s.window_size;p++){
				if(s.window_sign[p]<s.sign.length)
					System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN:"+s.window_sign[p]+" ||");
				else
					System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN: VACANT"+" ||");
			}
			System.out.println("=====================================");
			//PRINT RECEIVER WINDOW
			System.out.println("=======CURRENT RECEIVER WINDOW=======");
			for(int p=0;p<r.window_size;p++){
				if(r.window_sign[p]<r.sign.length)
					System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN:"+r.window_sign[p]+" ||");
				else
					System.out.println("|| WIN_NUMBER:"+p+"        DATA_SIGN: VACANT"+" ||");
			}
			System.out.println("=====================================\n");
			//IF SENDER OR RECEIVER WINDOW IS EMPTY, BREAK.
			if(r.window_sign[0]>=r.sign.length || i>=s.sign.length) break;
			//SEND DATA
			//CLAIM
			for(int count=0;count<s.window_size && count+i<s.sign.length ;count++){
				if(r.received[count]==0)
					System.out.println("Send data(sign:"+(i+count)+(")..."));
			}
			//COUNTER:RE_TIMES++(NOT MARKED)
			for(int count=0;count<s.window_size && count+i<s.sign.length;count++){
				if(r.received[count]==0)
					re_times[s.latest_ACK+1+count]++;
			}
			//SEND DATAS IN SENDER WINDOW

			for(int count=0;count<s.window_size && count+i<s.sign.length ;count++){
				//System.out.println("\nreceived:"+r.received[0]+r.received[1]+r.received[2]+r.received[3]);
				int m=0;
				//FIND SITE OF SEND_DATA
				for(m=0;m<r.window_size;m++){
					if(s.window_sign[count]==r.window_sign[m])
						break;
				}
				//IF NOT FOUND, BREAK.
				if(m==r.window_size) continue;
				//System.out.println("(sign:"+r.window_sign[m]+")");
				//IF NOT RECEIVED, SEND.
				if(r.received[m]==0){
					s.Time(count);
					//DELAY
					randelay=new Random().nextInt(5);
					Send_Delay(randelay);
					//P(LOSS/DAMAGED)
					ran=new Random().nextInt(3);
					//TELL RECEIVER DATA_SEQ, SENDER AND R.WIN_SEQ
					if(ran!=1) r.Receive(i+count,count);
					else System.out.println("data of Sign: "+(i+count)+" has lost.");
				}

			}
		}
		System.out.println("======Stastic of resend times:======");
		for(int count=0;count<s.sign.length;count++)
			System.out.println("||   Sign: "+count+"      RESEND_TIME:"+re_times[count]+"   ||");
		System.out.println("=====================================");
		System.exit(0);
	}
}