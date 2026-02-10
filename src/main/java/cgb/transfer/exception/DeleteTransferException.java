package cgb.transfer.exception;

public class DeleteTransferException extends TransferException{
	private static final long serialVersionUID = 1L;

	public enum FailureTransfert{
			OBJECT_NOT_FOUND,
			REMOVAL_FAILURE
	};
	
	public  DeleteTransferException(FailureTransfert ft) {
		// TODO Auto-generated constructor stub
		super(ft.name());
	}
	
	
}