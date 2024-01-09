package de.jplag.cli.logger;

import de.jplag.logging.ProgressBar;
import de.jplag.logging.ProgressBarProvider;
import de.jplag.logging.ProgressBarType;

import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class TongfeiProgressBarProvider implements ProgressBarProvider {
    @Override
    public ProgressBar initProgressBar(ProgressBarType type, int totalSteps) {
        me.tongfei.progressbar.ProgressBar progressBar = new ProgressBarBuilder().setTaskName(getProgressBarName(type)).setInitialMax(totalSteps)
                .setStyle(ProgressBarStyle.UNICODE_BLOCK).build();
        return new TongfeiProgressBar(progressBar);
    }

    private String getProgressBarName(ProgressBarType progressBarType) {
        return switch (progressBarType) {
            case LOADING -> "Loading Submissions  ";
            case PARSING -> "Parsing Submissions  ";
            case COMPARING -> "Comparing Submissions";
        };
    }
}
