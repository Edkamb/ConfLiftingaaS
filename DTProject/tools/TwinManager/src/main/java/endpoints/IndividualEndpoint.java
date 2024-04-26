package endpoints;

import config.TwinConfiguration;

public interface IndividualEndpoint extends Endpoint {
	public TwinConfiguration config = null;
	public String twinName = null;
	public String getTwinName();
}
