package org.sagebionetworks.markdown;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;

public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";
	public static final String OUTPUT = "output";
	public static final String RESULT = "result";

	@Autowired
	MarkdownClient markdownClient;

	@Override
	public String convertMarkdown(String rawMarkdown, String outputType) throws JSONException, ClientProtocolException, IOException, HttpClientHelperException {
		ValidateArgument.required(rawMarkdown, "rawMarkdown");
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		if (outputType != null) {
			request.put(OUTPUT, outputType);
		}
		JSONObject response = new JSONObject(markdownClient.requestMarkdownConversion(request.toString()));
		return response.getString(RESULT);
	}

}
