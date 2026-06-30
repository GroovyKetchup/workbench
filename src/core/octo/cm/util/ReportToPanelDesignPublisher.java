package octo.cm.util;

import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

/**
 * 报表设计发布到面板设计的处理器（一次性实例化使用）。
 *
 * <p>镜像 {@link FlowToPanelDesignPublisher}，复用公共基类
 * {@link AbstractToPanelDesignPublisher} 的面板物化逻辑，把报表定义一次性物化进目标面板。
 * 报表面板不涉及流程的面板状态/业务编排/面板权限，故只走通用顶层字段 + 面板角色 + 面板数据 +
 * 面板按钮 + 默认视图。</p>
 *
 * <p><b>双向关联：</b>发布后建立双向绑定：
 * <ul>
 *   <li>{@code 面板.Form.Owner = 报表定义.uuid}（本类负责）</li>
 *   <li>{@code 面板.面板描述 += “<!--REPORT_DEF_MODEL_ID:FormModelId-->”}（本类负责，HTML 注释隐藏标记）</li>
 *   <li>{@code 报表定义.”关联面板” = 面板编号}（调用方 {@link cell.octo.cm.expr.ReportDesignerExpr#publishToPanelDesign} 负责）</li>
 * </ul>
 * 运行态通过 Owner + 描述字段隐藏标记联合查询报表定义，支持多报表定义模型，跨环境自动迁移。</p>
 *
 * @author Devin
 * @version 1.0
 */
@Comment("报表设计发布到面板设计的处理器")
@ClassDeclare(
        label = "报表设计发布到面板设计的处理器",
        what = "把报表定义一次性物化进目标面板",
        why = "镜像流程发布；建立双向绑定（Owner + 描述字段隐藏标记 + 关联面板字段）",
        how = "继承 AbstractToPanelDesignPublisher，复用公共面板物化逻辑；描述字段追加 HTML 注释标记存 FormModelId",
        developer = "Devin", version = "1.0",
        createTime = "2026-06-25", updateTime = "2026-06-26"
)
public class ReportToPanelDesignPublisher extends AbstractToPanelDesignPublisher {

    /**
     * 构造报表发布器。
     *
     * @param dao       数据访问会话
     * @param observer  业务域观察者
     * @param modelType 模型类别（一般为”报表”）
     * @param reportDef 报表定义 Form（其 FormModelId 写入目标面板 Owner）
     * @param src       发布源数据（前端映射过来的面板 JSON）
     */
    public ReportToPanelDesignPublisher(IDao dao, OctoDomainOpObserver observer, String modelType, Form reportDef, JSONObject src) {
        super(dao, observer, modelType, reportDef, src);
    }

    /**
     * 报表面板分类：数据看板。
     *
     * @return 面板分类关联值
     * @throws Exception 查询分类失败
     */
    @Override
    protected AssociationData getPanelCategoryAc() throws Exception {
        return PanelCategoryUtil.getAssignCategoryAc(PanelCategoryUtil.CategoryType_DataBoard);
    }

    /**
     * 执行报表发布。
     *
     * @param existedPanelCode 已有的面板编号；为空或后台查无此面板时均走新建分支
     * @return 已落库的面板设计 Form
     * @throws Exception 发布失败
     */
    public Form publish(String existedPanelCode) throws Exception {
        locateOrCreatePanel(existedPanelCode);

        fillTopFields();
        fillPanelRoles();
        fillPanelData();
        fillPanelButtons();
        ensureDefaultCreateButton();
        ensureSystemDefaultButtons();
        buildDefaultViews();

        // 双向绑定：
        // 1. Owner 存报表定义 uuid（用于关联和联动删除）
        // 2. 面板描述末尾追加隐藏标记存 FormModelId（支持多报表定义模型，跨环境自动迁移）
        if (ownerDef != null) {
            panel.setAttrValueByCode(Form.Owner, ownerDef.getUuid());

            String panelDesc = panel.getString("面板描述");
            if (panelDesc == null) panelDesc = "";
            // 移除旧标记
            panelDesc = panelDesc.replaceAll("<!--REPORT_DEF_MODEL_ID:.*?-->", "");
            // 追加新标记（HTML 注释，前端不可见）
            String marker = "<!--REPORT_DEF_MODEL_ID:" + ownerDef.getFormModelId() + "-->";
            panel.setAttrValue("面板描述", panelDesc + marker);
        }

        panel = IFormMgr.get().updateForm(null, dao, panel, observer);
        dao.commit();
        return panel;
    }
}
