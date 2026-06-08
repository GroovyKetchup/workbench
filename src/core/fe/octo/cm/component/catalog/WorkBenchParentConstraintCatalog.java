package fe.octo.cm.component.catalog;

import cmn.anotation.ClassDeclare;
import fe.cmn.panel.BoxDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.tree.TreeNodeDto;
import fe.cmn.tree.TreeNodeQuerier;
import fe.cmn.tree.TreeNodeQuerierContext;
import fe.cmn.widget.WidgetDto;
import fe.util.component.param.TreeParam;
import octo.cm.constant.WorkBenchConst;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

import static octo.cm.constant.WorkBenchConst.LAYER_NAME_SCENE;

@Comment("父节点约束-OctoCM目录树")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchParentConstraintCatalog<T extends TreeParam> extends WorkBenchCatalog<T> {

    @Override
    public WidgetDto getWidget(PanelContext panelContext) throws Exception {
        return super.getWidget(panelContext);
    }

    @Override
    public BoxDto getTopBar(PanelContext context, T widgetParam) throws Exception {
        return BoxDto.vbar();
    }

    @Override
    public List<TreeNodeDto> queryChild(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        return super.queryChild(querier, context);
    }

    @Override
    public List<TreeNodeDto> doQueryAssignRoot(TreeNodeQuerier querier, TreeNodeQuerierContext context, String rootNodeType, String rootNodeKey) throws Exception {
        List<TreeNodeDto> resultList = new ArrayList<>();
        TreeNodeQuerier assignRootQuerier = new TreeNodeQuerier().setParentKey(rootNodeKey);

        // 为了查询自己, 临时使用这个做标志位
        assignRootQuerier.setUserObject(WorkBenchConst.SIGN_QUERY_SELF);
        List<TreeNodeDto> childScenes = getTreeNodeProcessor(LAYER_NAME_SCENE).queryTree(assignRootQuerier, context);


        resultList.addAll(childScenes);

        return resultList;

    }
}
