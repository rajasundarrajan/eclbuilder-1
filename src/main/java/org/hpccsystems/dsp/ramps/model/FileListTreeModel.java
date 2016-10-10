package org.hpccsystems.dsp.ramps.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.ramps.controller.utils.FileBrowserRetriver;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.AbstractTreeModel;

public class FileListTreeModel extends AbstractTreeModel<FileMeta> {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(FileListTreeModel.class);

    private HPCCConnection hpccConnection;
   

    public FileListTreeModel(FileMeta fileMeta, HPCCConnection hpccConnection) {
        super(fileMeta);
        super.setMultiple(true);
        this.hpccConnection = hpccConnection;
    }

    @Override
    public FileMeta getChild(FileMeta parent, int index) {
        if (index < parent.getChildlist().size()) {
            return parent.getChildlist().get(index);
        } else {
            return null;
        }
    }

    @Override
    public int getChildCount(FileMeta parent) {
        if (parent.getChildlist() == null) {
            try {
                parent.setChildlist(FileBrowserRetriver.getFileList(parent.getScope(),
                        hpccConnection, ((LogicalFileService)SpringUtil.getBean(
                                Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles()));
            } catch (Exception e) {
                LOG.error(Constants.EXCEPTION, e);
                return 0;
            }

        }
        return parent.getChildlist().size();
    }

    @Override
    public boolean isLeaf(FileMeta node) {
        return !node.isDirectory();
    }

}
