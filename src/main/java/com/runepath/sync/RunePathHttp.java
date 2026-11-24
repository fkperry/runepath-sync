package com.runepath.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class RunePathHttp
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String origin;

    RunePathHttp(OkHttpClient client, String origin)
    {
        this.client = client;
        this.origin = origin;
    }

    Result post(String route, JsonObject payload, String token) throws IOException
    {
        Request.Builder builder = new Request.Builder()
            .url(origin + "/_api/runepath/" + route)
            .header("User-Agent", "RunePath-Sync/0.1 (RuneLite)")
            .header("Accept", "application/json")
            .post(RequestBody.create(JSON, payload.toString()));
        if (token != null && !token.isEmpty())
        {
            builder.header("Authorization", "Bearer " + token);
        }
        try (Response response = client.newCall(builder.build()).execute())
        {
            ResponseBody body = response.body();
            String content = body == null ? "{}" : body.string();
            JsonObject parsed;
            try
            {
                parsed = JsonParser.parseString(content).getAsJsonObject();
            }
            catch (RuntimeException e)
            {
                throw new IOException("RunePath returned a non-JSON response.", e);
            }
            if (response.code() >= 500)
            {
                throw new IOException("RunePath server unavailable: HTTP " + response.code());
            }
            return new Result(response.code(), parsed);
        }
    }

    static final class Result
    {
        final int status;
        final JsonObject body;

        Result(int status, JsonObject body)
        {
            this.status = status;
            this.body = body;
        }

        String field(String key)
        {
            return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString() : "";
        }
    }
}
