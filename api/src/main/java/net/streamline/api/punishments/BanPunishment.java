package net.streamline.api.punishments;

import net.streamline.api.SLAPI;

public class BanPunishment extends AbstractPunishment {
    @Override
    public String type() {
        return "BAN";
    }

    public BanPunishment(String convict, String executioner) {
        super(convict, executioner);
    }

    @Override
    public void populateMore() {

    }

    @Override
    public void loadMore() {

    }

    @Override
    public void saveMore() {

    }
}
