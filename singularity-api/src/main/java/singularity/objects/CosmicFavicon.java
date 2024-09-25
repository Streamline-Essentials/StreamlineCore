package singularity.objects;

import com.google.common.io.BaseEncoding;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

@Getter
public class CosmicFavicon {
    private static final TypeAdapter<CosmicFavicon> FAVICON_TYPE_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, CosmicFavicon value) throws IOException {
            TypeAdapters.STRING.write(out, value == null ? null : value.getEncoded());
        }

        @Override
        public CosmicFavicon read(JsonReader in) throws IOException {
            String enc = TypeAdapters.STRING.read(in);
            return enc == null ? null : create(enc);
        }
    };

    public CosmicFavicon(@NonNull String encoded) {
        this.encoded = encoded;
    }

    public static TypeAdapter<CosmicFavicon> getFaviconTypeAdapter()
    {
        return FAVICON_TYPE_ADAPTER;
    }

    /**
     * The base64 encoded favicon, including MIME header.
     */
    @NonNull
    private final String encoded;

    /**
     * Creates a favicon from an image.
     *
     * @param image the image to create on
     * @return the created favicon instance
     * @throws IllegalArgumentException if the favicon is larger than
     * {@link Short#MAX_VALUE} or not of dimensions 64x64 pixels.
     */
    public static CosmicFavicon create(BufferedImage image)
    {
        // check size
        if ( image.getWidth() != 64 || image.getHeight() != 64 )
        {
            throw new IllegalArgumentException( "Server icon must be exactly 64x64 pixels" );
        }

        // dump image PNG
        byte[] imageBytes;
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write( image, "PNG", stream );
            imageBytes = stream.toByteArray();
        } catch ( IOException e )
        {
            // ByteArrayOutputStream should never throw this
            throw new AssertionError( e );
        }

        // encode with header
        String encoded = "data:image/png;base64," + BaseEncoding.base64().encode( imageBytes );

        // check encoded image size
        if ( encoded.length() > Short.MAX_VALUE )
        {
            throw new IllegalArgumentException( "Favicon file too large for server to process" );
        }

        // create
        return new CosmicFavicon( encoded );
    }

    /**
     * Creates a Favicon from an encoded PNG.
     *
     * @param encodedString a base64 mime encoded PNG string
     * @return the created favicon
     * @deprecated Use #create(java.awt.image.BufferedImage) instead
     */
    @Deprecated
    public static CosmicFavicon create(String encodedString)
    {
        return new CosmicFavicon( encodedString );
    }

    public static CosmicFavicon createFromURL(URL url) throws IOException {
        return create(ImageIO.read(url));
    }
}
