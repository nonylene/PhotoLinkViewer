package net.nonylene.photolinkviewer.tool;

import java.util.ArrayList;

public class AccountsList {
    private ArrayList<String> screen_list;
    private ArrayList<Integer> row_id_list;

    public ArrayList<Integer> getRowIdList() {
        return row_id_list;
    }

    public ArrayList<String> getScreenList() {
        return screen_list;
    }

    public void setRowIdList(ArrayList<Integer> row_id_list) {
        this.row_id_list = row_id_list;
    }

    public void setScreenList(ArrayList<String> screen_list) {
        this.screen_list = screen_list;
    }
}
