public class ActionFailedException extends Exception {
	String msg ;

	public ActionFailedException(String msg){
		this.msg = msg;
	}
	
	public String getMessage(){
		return this.msg;
	}
}
