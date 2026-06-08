package octo.cm.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.json.JSONUtil;
import fe.cmn.data.CColor;
import fe.cmn.menu.MenuDecorationDto;
import fe.cmn.menu.MenuDto;
import fe.cmn.menu.MenuItemDecorationDto;
import fe.cmn.menu.MenuItemDto;
import fe.cmn.text.CFontWeight;
import fe.cmn.text.CTextStyle;
import fe.cmn.tree.TreeDto;
import fe.cmn.tree.decoration.TreeDecorationDto;
import fe.cmn.tree.decoration.TreeNodeDecorationDto;
import fe.cmn.widget.InsetDto;
import fe.cmn.widget.decoration.*;
import octo.cm.enums.DefaultSystemModule;
import org.nutz.dao.entity.annotation.Comment;

import java.awt.*;

@Comment("统一样式工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-24", updateTime = "2025-06-24"
)
public class UniformStyleUtil {


    public static final Color MAIN_COLOR = new Color(209, 213, 218);

    public static void setTreeStyle(TreeDto tree) {
        if (tree == null) return;

        double fontSize = 15D;
        CTextStyle defaultTextStyle = new CTextStyle().setFontSize(fontSize)
//                .setFontWeight(CFontWeight.bold)
                .setColor(
                        new Color(102, 105, 104)
                );

//        Color selectedColor = new Color(255, 243 - (11 * 10), 236 - (17 * 10));
        CTextStyle selectedTextStyle = new CTextStyle().setFontSize(fontSize)
//                .setFontWeight(CFontWeight.bold)
//                .setColor(MAIN_COLOR)
                ;

        // 默认图标
        IconStyleDto defaultNodeIconStyle = new IconStyleDto()
                .setIconColor(Color.BLACK)
                .setSize(20D);
        // 选中图标
        IconStyleDto selectedNodeIconStyle = new IconStyleDto()
//                .setIconColor(MAIN_COLOR)
                .setSize(20D);

        // 默认的节点装饰
        TreeNodeDecorationDto defaultNodeDecoration = new TreeNodeDecorationDto();
        defaultNodeDecoration.setTextStyle(defaultTextStyle)
                .setIconStyle(defaultNodeIconStyle)
                .setIconRightPadding(10D)
                .setBorder(new BorderDto().setLeft(new BorderSideDto(Color.WHITE, 5D)));

        // Hover的节点装饰
        TreeNodeDecorationDto hoverNodeDecoration = new TreeNodeDecorationDto();
        hoverNodeDecoration.setTextStyle(selectedTextStyle)
                .setIconStyle(selectedNodeIconStyle)
                .setIconRightPadding(10D)
                .setBorder(new BorderDto().setLeft(new BorderSideDto(Color.WHITE, 5D)));


        // 选中的节点装饰
        Color selectedColor = Color.WHITE;

        TreeNodeDecorationDto selectedNodeDecoration = new TreeNodeDecorationDto();
        selectedNodeDecoration.setTextStyle(selectedTextStyle);
        selectedNodeDecoration
                .setBackground(CColor.fromColor(selectedColor))
                .setIconStyle(selectedNodeIconStyle)
                .setBorder(new BorderDto().setLeft(new BorderSideDto(MAIN_COLOR, 5D)));

        // 树的装饰类
        TreeDecorationDto treeDecoration = new TreeDecorationDto();
        treeDecoration.setNodeHeight(40D)
                .setDefalutNodeDecorationDto(defaultNodeDecoration)
                .setHoverNodeDecorationDto(hoverNodeDecoration)
                .setSelectedNodeDecorationDto(selectedNodeDecoration)
                .setExpandIconOnFront(false);

//                .setNodeIconStyle(defaultNodeIconStyle);

        tree.setDecoration(treeDecoration);

    }

    public static void setMenuStyle(MenuDto menu) {
        if (menu == null) return;
        menu.setPreferWidth(150d);
        MenuDecorationDto dto = new MenuDecorationDto();
        dto.setPadding(InsetDto.all(15d));
        dto.setMargin(InsetDto.all(15d));
        dto.setBackgroundColor(Color.WHITE);
        dto.setShadowColor(new CColor(0, 0, 0, 0.15f));
        dto.setBorderRadius(BorderRadiusDto.all(RadiusDto.circular(10D)));
        menu.setDecoration(dto);

    }

    public static void setMenuItemStyle(MenuItemDto itemDto) {
        if (itemDto == null) return;

        itemDto.setMenuItemDecoration(
                new MenuItemDecorationDto().setStartPadding(10d).setEndPadding(10d)
                        .setHeight(40d)
                        .setBackground(Color.WHITE)
                        .setIconDecoration((IconDecorationDto) new IconDecorationDto().setSize(19d).setIconColor(Color.BLACK).setMargin(new InsetDto().setRight(20d)))
                        .setTextStyle(new CTextStyle().setColor(new CColor(20, 20, 20, 0.7f)).setFontSize(15d).setFontWeight(CFontWeight.w600))
        );
        itemDto.setHighlightMenuItemDecoration(
                new MenuItemDecorationDto().setStartPadding(10d).setEndPadding(10d)
                        .setHeight(40d)
//                        .setBackground(new Color(255, 135, 0))
                        .setBackground(Color.WHITE)
//                        .setBorderRadius(BorderRadiusDto.all(RadiusDto.circular(8D)))
                        .setIconDecoration((IconDecorationDto) new IconDecorationDto().setSize(19d)
//                                .setIconColor(new Color(255, 135, 0))
                                        .setMargin(new InsetDto().setRight(20d))
                        )
                        .setTextStyle(new CTextStyle()
//                                .setColor(new Color(255, 135, 0))
                                .setFontSize(15d).setFontWeight(CFontWeight.w600))
        );
    }



}
