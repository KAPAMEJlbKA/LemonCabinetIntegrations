package simplecabinet.api;

public class SimpleCabinetResponse<R> {
    public R data;
    public int code;
    public String error;

    public SimpleCabinetResponse(R data, int code, String error) {
        this.data = data;
        this.code = code;
        this.error = error;
    }

    public R getOrThrow() {
        if(code == 404) {
            return null;
        }
        if(error != null) {
            throw new SimpleCabinetAPI.SimpleCabinetException(error);
        }
        return data;
    }

    public boolean isSuccess() {
        if(code == 404) {
            return true;
        }
        return error == null;
    }
}
