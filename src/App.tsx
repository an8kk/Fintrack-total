import { Authenticated, Refine } from "@refinedev/core";
import { RefineKbar, RefineKbarProvider } from "@refinedev/kbar";

import {
  ErrorComponent,
  ThemedLayout,
  ThemedSider,
  useNotificationProvider,
} from "@refinedev/antd";
import "@refinedev/antd/dist/reset.css";

import routerProvider, {
  CatchAllNavigate,
  DocumentTitleHandler,
  NavigateToResource,
  UnsavedChangesNotifier,
} from "@refinedev/react-router";
import { App as AntdApp } from "antd";
import { BrowserRouter, Outlet, Route, Routes } from "react-router";
import { Header } from "./components/header";
import { ColorModeContextProvider } from "./contexts/color-mode";
import { Dashboard } from "./pages/dashboard";
import { UserList, UserShow, UserEdit, UserCreate } from "./pages/users";
import { TransactionList, TransactionCreate } from "./pages/transactions";
import { AuditLogList } from "./pages/audit-logs";
import { MerchantMapList } from "./pages/merchant-maps";
import { GlobalNotification } from "./pages/notifications";
import { AiInsights } from "./pages/ai-insights";
import { Login } from "./pages/login";
import { authProvider } from "./providers/auth";
import { dataProvider } from "./providers/data";
import {
  DashboardOutlined,
  TeamOutlined,
  AuditOutlined,
  TagsOutlined,
  ExperimentOutlined,
  NotificationOutlined,
} from "@ant-design/icons";

function App() {
  return (
    <BrowserRouter>
      <RefineKbarProvider>
        <ColorModeContextProvider>
          <AntdApp>
            <Refine
              dataProvider={dataProvider}
              notificationProvider={useNotificationProvider}
              routerProvider={routerProvider}
              authProvider={authProvider}
              resources={[
                {
                  name: "dashboard",
                  list: "/",
                  meta: {
                    label: "Dashboard",
                    icon: <DashboardOutlined />,
                  },
                },
                {
                  name: "users",
                  list: "/users",
                  create: "/users/create",
                  show: "/users/show/:id",
                  edit: "/users/edit/:id",
                  meta: {
                    canDelete: true,
                    label: "Users",
                    icon: <TeamOutlined />,
                  },
                },
                {
                  name: "audit-logs",
                  list: "/audit-logs",
                  meta: {
                    label: "Audit Logs",
                    icon: <AuditOutlined />,
                  },
                },
                {
                  name: "merchant-maps",
                  list: "/merchant-maps",
                  meta: {
                    label: "Merchant Maps",
                    icon: <TagsOutlined />,
                  },
                },
                {
                  name: "ai-insights",
                  list: "/ai-insights",
                  meta: {
                    label: "AI Insights",
                    icon: <ExperimentOutlined />,
                  },
                },
                {
                  name: "notifications",
                  list: "/notifications",
                  meta: {
                    label: "Broadcast",
                    icon: <NotificationOutlined />,
                  },
                },
              ]}
              options={{
                syncWithLocation: true,
                warnWhenUnsavedChanges: true,
                title: {
                  text: "FinTrack Admin",
                  icon: "💰",
                },
              }}
            >
              <Routes>
                <Route
                  element={
                    <Authenticated
                      key="authenticated-inner"
                      fallback={<CatchAllNavigate to="/login" />}
                    >
                      <ThemedLayout
                        Header={Header}
                        Sider={(props) => (
                          <ThemedSider {...props} fixed />
                        )}
                      >
                        <Outlet />
                      </ThemedLayout>
                    </Authenticated>
                  }
                >
                  <Route index element={<Dashboard />} />
                  <Route path="/users">
                    <Route index element={<UserList />} />
                    <Route path="create" element={<UserCreate />} />
                    <Route path="show/:id" element={<UserShow />} />
                    <Route path="edit/:id" element={<UserEdit />} />
                    <Route path=":userId/transactions" element={<TransactionList />} />
                    <Route path=":userId/transactions/create" element={<TransactionCreate />} />
                  </Route>
                  <Route path="/audit-logs" element={<AuditLogList />} />
                  <Route path="/merchant-maps" element={<MerchantMapList />} />
                  <Route path="/ai-insights" element={<AiInsights />} />
                  <Route path="/notifications" element={<GlobalNotification />} />
                  <Route path="*" element={<ErrorComponent />} />
                </Route>
                <Route
                  element={
                    <Authenticated
                      key="authenticated-outer"
                      fallback={<Outlet />}
                    >
                      <NavigateToResource />
                    </Authenticated>
                  }
                >
                  <Route path="/login" element={<Login />} />
                </Route>
              </Routes>

              <RefineKbar />
              <UnsavedChangesNotifier />
              <DocumentTitleHandler />
            </Refine>
          </AntdApp>
        </ColorModeContextProvider>
      </RefineKbarProvider>
    </BrowserRouter>
  );
}

export default App;
