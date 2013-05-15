package rc;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.connection.ServerDetails;
import com.pushtechnology.diffusion.api.remote.RemoteService;

public abstract class BaseService {

	protected ServerDetails serverDetails = null;
	protected RemoteService service = null;
	protected BaseRemoteListener listener = null;
	
	public ServerDetails getServerDetails() {
		if(serverDetails == null) {
			try {
				serverDetails = ConnectionFactory.createServerDetails("dpt://localhost:8080");
			}
			catch(APIException ex) {
				Logs.warning("Failed to create server details", ex);
			}
		}
		return serverDetails;
	}
	
	public String getControlTopicName() {
		return "RemoteControl";
	}
	
	public abstract String getDomainTopicName();
	
	public RemoteService getRemoteService() {
		return service;
	}
}
