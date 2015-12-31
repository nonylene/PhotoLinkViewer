package net.nonylene.photolinkviewer.event

import android.view.View
import net.nonylene.photolinkviewer.tool.PLVUrl

public class DownloadButtonEvent(val plvUrl: PLVUrl)
public class ShowFragmentEvent(val isToBeShown: Boolean)
public class SnackbarEvent(val message: String, val actionMessage: String?, val actionListener: (view: View) -> Unit)
