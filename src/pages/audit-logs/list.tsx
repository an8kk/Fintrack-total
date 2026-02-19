import { useCustom } from "@refinedev/core";
import { List } from "@refinedev/antd";
import { Table, Tag, Typography } from "antd";
import { API_URL } from "../../providers/constants";

const { Text } = Typography;

export const AuditLogList = () => {
    const result = useCustom<any>({
        url: `${API_URL}/admin/audit-logs`,
        method: "get",
        config: { query: { page: 0, size: 100 } },
    });

    const pageData = (result as any)?.data?.data ?? (result as any)?.query?.data?.data;
    const data = pageData?.content ?? [];
    const isLoading = (result as any)?.isLoading ?? (result as any)?.query?.isLoading ?? false;

    const actionColors: Record<string, string> = {
        DELETE_USER: "red",
        SALT_EDGE_SESSION: "blue",
        GLOBAL_NOTIFICATION: "purple",
        CREATE_MERCHANT_MAP: "green",
        DELETE_MERCHANT_MAP: "orange",
    };

    return (
        <List title="Audit Logs">
            <Table dataSource={data} loading={isLoading} rowKey="id" size="small">
                <Table.Column
                    dataIndex="timestamp"
                    title="Time"
                    render={(val: string) =>
                        val ? new Date(val).toLocaleString() : "—"
                    }
                    width={180}
                />
                <Table.Column dataIndex="adminEmail" title="Admin" width={200} />
                <Table.Column
                    dataIndex="action"
                    title="Action"
                    render={(val: string) => (
                        <Tag color={actionColors[val] || "default"}>{val}</Tag>
                    )}
                    width={200}
                />
                <Table.Column dataIndex="targetType" title="Target" width={140} />
                <Table.Column dataIndex="targetId" title="ID" width={60} />
                <Table.Column
                    dataIndex="details"
                    title="Details"
                    render={(val: string) => (
                        <Text ellipsis={{ tooltip: val }} style={{ maxWidth: 300 }}>
                            {val}
                        </Text>
                    )}
                />
            </Table>
        </List>
    );
};
