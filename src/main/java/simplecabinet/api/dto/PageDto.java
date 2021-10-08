package simplecabinet.api.dto;

import java.util.List;

public class PageDto<T> {
    public List<T> data;
    public int pageSize;
    public int totalPages;
    public long totalElements;
}
