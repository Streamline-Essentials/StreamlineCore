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

/**
 * Represents a server favicon — a 64×64 pixel PNG image encoded as a
 * Base64 data-URI string that can be sent to clients in the server-list
 * ping response.
 *
 * <p>Instances are created via the static factory methods
 * ({@link #create(BufferedImage)}, {@link #createFromURL(URL)},
 * {@link #createFromURL(String)}) and can be serialised/deserialised to
 * JSON through the {@link TypeAdapter} returned by
 * {@link #getFaviconTypeAdapter()}.
 */
@Getter @Setter
public class CosmicFavicon {

    /**
     * Gson {@link TypeAdapter} that serialises a {@link CosmicFavicon} as its
     * Base64 data-URI string and deserialises a data-URI string back into a
     * {@link CosmicFavicon}.  During deserialisation the encoded bytes are
     * decoded, the resulting image is verified to be exactly 64×64 pixels, and
     * the overall encoded length is checked against {@link Short#MAX_VALUE}.
     */
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

    /**
     * Constructs a favicon from a pre-encoded Base64 data-URI string and the
     * corresponding {@link BufferedImage}.
     *
     * @param encoded the {@code data:image/png;base64,...} encoded string;
     *                must not be {@code null}
     * @param image   the decoded image; must not be {@code null}
     */
    public CosmicFavicon(@NonNull String encoded, @NonNull BufferedImage image) {
        this.encoded = encoded;
        this.image = image;
    }

    /**
     * Returns the Gson {@link TypeAdapter} capable of reading and writing
     * {@link CosmicFavicon} instances as Base64 data-URI JSON strings.
     *
     * @return the favicon type adapter
     */
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
     * The decoded {@link BufferedImage} that this favicon represents; always a
     * 64×64 pixel PNG.
     */
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

    /**
     * Downloads an image from the given {@link URL} and creates a favicon from
     * it.  Returns {@code null} if the download or image read fails for any
     * reason.
     *
     * @param url the URL of the image to fetch
     * @return the created {@link CosmicFavicon}, or {@code null} on failure
     */
    public static CosmicFavicon createFromURL(URL url) {
        try {
            return create(ImageIO.read(url));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Parses the given string as a URL, downloads the image at that location,
     * and creates a favicon from it.  Returns {@code null} if the URL is
     * malformed or if downloading/reading the image fails.
     *
     * @param url the URL string of the image to fetch
     * @return the created {@link CosmicFavicon}, or {@code null} on failure
     */
    public static CosmicFavicon createFromURL(String url) {
        try {
            return createFromURL(URI.create(url).toURL());
        } catch (Throwable e) {
            return null;
        }
    }
}
