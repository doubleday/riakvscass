package "s3cmd"

bucket_creds = Chef::EncryptedDataBagItem.load("s3", "wooga-g9-backup")

template "/home/ubuntu/.s3cfg" do
  source "s3cfg.erb" 
  mode 00644
  owner "ubuntu"
  group "ubuntu"
  variables(
    :access_key => bucket_creds["access_key"],
    :secret_key => bucket_creds["secret_key"]
  )
end
