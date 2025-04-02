package singularity.objects;

import com.google.common.io.BaseEncoding;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

@Getter @Setter
public class CosmicFavicon {
    private static final TypeAdapter<CosmicFavicon> FAVICON_TYPE_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, CosmicFavicon value) throws IOException {
            TypeAdapters.STRING.write(out, value == null ? null : value.getEncoded());
        }

        @Override
        public CosmicFavicon read(JsonReader in) throws IOException {
            String enc = TypeAdapters.STRING.read(in);
            if ( enc == null )
            {
                return null;
            }

            // decode
            byte[] imageBytes = BaseEncoding.base64().decode( enc.substring( "data:image/png;base64,".length() ) );
            BufferedImage image;
            try
            {
                image = ImageIO.read( new ByteArrayInputStream( imageBytes ) );
            } catch ( IOException e )
            {
                throw new IOException( "Failed to decode favicon", e );
            }

            // check size
            if ( image.getWidth() != 64 || image.getHeight() != 64 )
            {
                throw new IOException( "Favicon must be exactly 64x64 pixels" );
            }

            // create
            CosmicFavicon favicon = new CosmicFavicon( enc, image );
            if ( favicon.getEncoded().length() > Short.MAX_VALUE )
            {
                throw new IOException( "Favicon file too large for server to process" );
            }
            return favicon;
        }
    };

    public CosmicFavicon(@NonNull String encoded, @NonNull BufferedImage image) {
        this.encoded = encoded;
        this.image = image;
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

    @NonNull
    private final BufferedImage image;

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
        return new CosmicFavicon( encoded, image );
    }

    public static CosmicFavicon createFromURL(URL url) throws IOException {
        return create(ImageIO.read(url));
    }

    public static CosmicFavicon createFromURL(String url) throws IOException {
        return createFromURL(URI.create(url).toURL());
    }
}
