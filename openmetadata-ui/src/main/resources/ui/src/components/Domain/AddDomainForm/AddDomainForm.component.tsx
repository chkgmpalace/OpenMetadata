/*
 *  Copyright 2023 Collate.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { Button, Form, FormProps, Space } from 'antd';
import { omit } from 'lodash';
import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ENTITY_NAME_REGEX,
  HEX_COLOR_CODE_REGEX,
} from '../../../constants/regex.constants';
import { usePermissionProvider } from '../../../context/PermissionProvider/PermissionProvider';
import { ResourceEntity } from '../../../context/PermissionProvider/PermissionProvider.interface';
import { CreateDataProduct } from '../../../generated/api/domains/createDataProduct';
import {
  CreateDomain,
  DomainType,
} from '../../../generated/api/domains/createDomain';
import { Operation } from '../../../generated/entity/policies/policy';
import { EntityReference } from '../../../generated/entity/type';
import { FieldProp, FieldTypes } from '../../../interface/FormUtils.interface';
import { domainTypeTooltipDataRender } from '../../../utils/DomainUtils';
import { generateFormFields, getField } from '../../../utils/formUtils';
import { checkPermission } from '../../../utils/PermissionsUtils';
import '../domain.less';
import { DomainFormType } from '../DomainPage.interface';
import { AddDomainFormProps } from './AddDomainForm.interface';

const AddDomainForm = ({
  isFormInDialog,
  loading,
  onCancel,
  onSubmit,
  formRef,
  type,
}: AddDomainFormProps) => {
  const { t } = useTranslation();
  const [form] = Form.useForm(formRef);
  const { permissions } = usePermissionProvider();

  const domainTypeArray = Object.keys(DomainType).map((key) => ({
    key,
    value: DomainType[key as keyof typeof DomainType],
  }));

  const formFields: FieldProp[] = [
    {
      name: 'name',
      id: 'root/name',
      label: t('label.name'),
      required: true,
      placeholder: t('label.name'),
      type: FieldTypes.TEXT,
      props: {
        'data-testid': 'name',
      },
      rules: [
        {
          pattern: ENTITY_NAME_REGEX,
          message: t('message.entity-name-validation'),
        },
        {
          min: 1,
          max: 128,
          message: `${t('message.entity-maximum-size', {
            entity: `${t('label.name')}`,
            max: '128',
          })}`,
        },
      ],
    },
    {
      name: 'displayName',
      id: 'root/displayName',
      label: t('label.display-name'),
      required: false,
      placeholder: t('label.display-name'),
      type: FieldTypes.TEXT,
      props: {
        'data-testid': 'display-name',
      },
    },
    {
      name: 'description',
      required: true,
      label: t('label.description'),
      id: 'root/description',
      type: FieldTypes.DESCRIPTION,
      props: {
        'data-testid': 'description',
        initialValue: '',
        height: 'auto',
      },
    },
    {
      name: 'iconURL',
      id: 'root/iconURL',
      label: t('label.icon-url'),
      required: false,
      placeholder: t('label.icon-url'),
      type: FieldTypes.TEXT,
      helperText: t('message.govern-url-size-message'),
      props: {
        'data-testid': 'icon-url',
      },
    },
    {
      name: 'color',
      id: 'root/color',
      label: t('label.color'),
      required: false,
      type: FieldTypes.COLOR_PICKER,
      rules: [
        {
          pattern: HEX_COLOR_CODE_REGEX,
          message: t('message.hex-color-validation'),
        },
      ],
    },
  ];

  if (type === DomainFormType.DOMAIN) {
    const domainTypeField: FieldProp = {
      name: 'domainType',
      required: true,
      label: t('label.domain-type'),
      id: 'root/domainType',
      type: FieldTypes.SELECT,
      helperText: domainTypeTooltipDataRender(),
      props: {
        'data-testid': 'domainType',
        options: domainTypeArray,
        overlayClassName: 'domain-type-tooltip-container',
        tooltipPlacement: 'topLeft',
        tooltipAlign: { targetOffset: [18, 0] },
      },
    };

    formFields.push(domainTypeField);
  }

  const ownerField: FieldProp = {
    name: 'owner',
    id: 'root/owner',
    required: false,
    label: t('label.owner'),
    type: FieldTypes.USER_MULTI_SELECT,
    props: {
      userOnly: false,
      allowMultiple: false,
    },
    formItemProps: {
      valuePropName: 'owners',
      trigger: 'onUpdate',
    },
  };

  const expertsField: FieldProp = {
    name: 'experts',
    id: 'root/experts',
    required: false,
    label: t('label.expert-plural'),
    type: FieldTypes.USER_MULTI_SELECT,
    props: {
      userOnly: true,
      allowMultiple: true,
    },
    formItemProps: {
      valuePropName: 'owners',
      trigger: 'onUpdate',
      initialValue: [],
    },
  };

  const createPermission = useMemo(
    () =>
      checkPermission(Operation.Create, ResourceEntity.GLOSSARY, permissions),
    [permissions]
  );

  const expertsList = Form.useWatch<EntityReference[]>('experts', form) ?? [];

  const handleFormSubmit: FormProps['onFinish'] = (formData) => {
    const updatedData = omit(formData, 'color', 'iconURL');
    const style = {
      color: formData.color,
      iconURL: formData.iconURL,
    };
    const data = {
      ...updatedData,
      style,
      experts: expertsList.map((item) => item.name ?? ''),
    } as CreateDomain | CreateDataProduct;

    onSubmit(data);
  };

  return (
    <Form
      data-testid="add-domain"
      form={form}
      layout="vertical"
      onFinish={handleFormSubmit}>
      {generateFormFields(formFields)}
      <div className="m-t-xss">{getField(ownerField)}</div>
      <div className="m-t-xss">{getField(expertsField)}</div>

      {!isFormInDialog && (
        <Space
          className="w-full justify-end"
          data-testid="cta-buttons"
          size={16}>
          <Button data-testid="cancel-domain" type="link" onClick={onCancel}>
            {t('label.cancel')}
          </Button>
          <Button
            data-testid="save-domain"
            disabled={!createPermission}
            htmlType="submit"
            loading={loading}
            type="primary">
            {t('label.save')}
          </Button>
        </Space>
      )}
    </Form>
  );
};

export default AddDomainForm;
