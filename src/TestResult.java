public class TestResult {
	private int exitCode = -1;
	
	public TestResult(int code){
		System.out.println(code);
		this.exitCode = code;
	}
	
	public boolean isPass(){
		if(this.exitCode == -1){
			throw new RuntimeException("Failed in get test result");
		}else if(this.exitCode == 0){
			return true;
		}else{
			return false;
		}
//		return this.status.equals("Failed")?false:true;
	}
}