package edu.illinois.library.cantaloupe.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class ImageResource extends AbstractResource {

    class ImageRepresentation extends OutputRepresentation {

        InputStream inputStream;
        Parameters params;

        public ImageRepresentation(MediaType mediaType, Parameters params,
                                   InputStream inputStream) {
            super(mediaType);
            this.inputStream = inputStream;
            this.params = params;
        }

        public void write(OutputStream outputStream) throws IOException {
            try {
                Processor proc = ProcessorFactory.getProcessor();
                proc.process(this.params, inputStream, outputStream);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    @Get
    public Representation doGet() throws IllegalArgumentException,
            UnsupportedEncodingException, FileNotFoundException {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = java.net.URLDecoder.
                decode((String) attrs.get("identifier"), "UTF-8");
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        Resolver resolver = ResolverFactory.getResolver();
        InputStream inputStream = resolver.resolve(identifier);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found");
        }

        Processor proc = ProcessorFactory.getProcessor();
        if (!proc.getSupportedOutputFormats().contains(params.getOutputFormat())) {
            String msg = String.format("%s supports only the following formats: %s",
                    proc, StringUtils.join(proc.getSupportedOutputFormats(), ", "));
            throw new IllegalArgumentException(msg);
        }

        this.addHeader("Link",
                "<" + params.getCanonicalUri(this.getRootRef().toString()) +
                        ">;rel=\"canonical\"");

        MediaType mediaType = new MediaType(
                OutputFormat.valueOf(format.toUpperCase()).getMediaType());
        return new ImageRepresentation(mediaType, params, inputStream);
    }

}
