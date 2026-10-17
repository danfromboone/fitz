package dbf.di;

public interface Controller {
    boolean canHandle(HttpRequest request);

    HttpResponse handle(HttpRequest request);
}
