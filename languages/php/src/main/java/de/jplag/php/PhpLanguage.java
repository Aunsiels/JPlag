package de.jplag.php;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.kohsuke.MetaInfServices;

import de.jplag.Language;
import de.jplag.ParsingException;
import de.jplag.Token;
import de.jplag.commentextraction.CommentExtractorSettings;
import de.jplag.commentextraction.EnvironmentDelimiter;

/**
 * Language implementation for PHP.
 */
@MetaInfServices(Language.class)
public class PhpLanguage implements Language {

    private final PhpParser parser = new PhpParser();

    @Override
    public List<String> fileExtensions() {
        return List.of(".php", ".phtml", ".php3", ".php4", ".php5", ".php7", ".phps", ".phpt");
    }

    @Override
    public String getName() {
        return "PHP";
    }

    @Override
    public String getIdentifier() {
        return "php";
    }

    @Override
    public int minimumTokenMatch() {
        return 12;
    }

    @Override
    public List<Token> parse(Set<File> files, boolean normalize) throws ParsingException {
        return parser.parse(files);
    }

    @Override
    public Optional<CommentExtractorSettings> getCommentExtractorSettings() {
        return Optional.of(new CommentExtractorSettings(List.of(new EnvironmentDelimiter("\""), new EnvironmentDelimiter("'")), List.of("//", "#"),
                List.of(new EnvironmentDelimiter("/*", "*/")), List.of("\\")));
    }
}
