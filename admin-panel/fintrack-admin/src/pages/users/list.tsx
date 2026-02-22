import {
    List,
    useTable,
    EditButton,
    ShowButton,
    DeleteButton,
    FilterDropdown,
} from "@refinedev/antd";
import { type BaseRecord } from "@refinedev/core";
import { Table, Space, Tag, Input } from "antd";

export const UserList = () => {
    const { tableProps } = useTable({
        syncWithLocation: true,
        resource: "users",
        filters: {
            initial: [{ field: "search", operator: "contains", value: "" }],
        },
    });

    return (
        <List title="Users">
            <Table {...tableProps} rowKey="id">
                <Table.Column dataIndex="id" title="ID" sorter width={80} />
                <Table.Column
                    dataIndex="username"
                    title="Username"
                    filterDropdown={(props) => (
                        <FilterDropdown {...props}>
                            <Input placeholder="Search users" />
                        </FilterDropdown>
                    )}
                />
                <Table.Column dataIndex="email" title="Email" />
                <Table.Column
                    dataIndex="role"
                    title="Role"
                    render={(value: string) => (
                        <Tag color={value === "ADMIN" ? "gold" : "blue"}>{value}</Tag>
                    )}
                    width={120}
                />
                <Table.Column
                    dataIndex="blocked"
                    title="Status"
                    render={(value: boolean) => (
                        <Tag color={value ? "red" : "green"}>
                            {value ? "Blocked" : "Active"}
                        </Tag>
                    )}
                    width={100}
                />
                <Table.Column
                    title="Actions"
                    dataIndex="actions"
                    render={(_, record: BaseRecord) => (
                        <Space>
                            <ShowButton hideText size="small" recordItemId={record.id} />
                            <EditButton hideText size="small" recordItemId={record.id} />
                            <DeleteButton hideText size="small" recordItemId={record.id} />
                        </Space>
                    )}
                    width={150}
                />
            </Table>
        </List>
    );
};
