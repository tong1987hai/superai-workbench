package com.superai.workbench;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.superai.workbench.ui.ai.ChatFragment;
import com.superai.workbench.ui.ai.PaintFragment;
import com.superai.workbench.ui.agent.AgentFragment;
import com.superai.workbench.ui.dashboard.DashboardFragment;
import com.superai.workbench.ui.im.IMFragment;
import com.superai.workbench.ui.knowledge.KnowledgeFragment;
import com.superai.workbench.ui.live.LiveFragment;
import com.superai.workbench.ui.pay.PayFragment;
import com.superai.workbench.ui.security.SecurityFragment;
import com.superai.workbench.ui.settings.SettingsActivity;
import com.superai.workbench.ui.video.VideoFragment;

/**
 * 主Activity - 采用 BottomNavigation + DrawerLayout 双导航架构
 * 底部导航：核心AI功能（对话/绘画/视频/知识库/智能体）
 * 侧边栏：管理功能（IM/直播/支付/看板/安全/设置）
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView sideNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNav = findViewById(R.id.bottom_navigation);
        sideNav = findViewById(R.id.side_navigation);

        setupBottomNavigation();
        setupSideNavigation();

        // 默认显示AI对话
        if (savedInstanceState == null) {
            loadFragment(new ChatFragment());
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                fragment = new ChatFragment();
            } else if (itemId == R.id.nav_paint) {
                fragment = new PaintFragment();
            } else if (itemId == R.id.nav_video) {
                fragment = new VideoFragment();
            } else if (itemId == R.id.nav_knowledge) {
                fragment = new KnowledgeFragment();
            } else if (itemId == R.id.nav_agent) {
                fragment = new AgentFragment();
            }
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void setupSideNavigation() {
        sideNav.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_im) {
                loadFragment(new IMFragment());
            } else if (itemId == R.id.nav_live) {
                loadFragment(new LiveFragment());
            } else if (itemId == R.id.nav_pay) {
                loadFragment(new PayFragment());
            } else if (itemId == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
            } else if (itemId == R.id.nav_security) {
                loadFragment(new SecurityFragment());
            } else if (itemId == R.id.nav_settings) {
                SettingsActivity.start(this);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
