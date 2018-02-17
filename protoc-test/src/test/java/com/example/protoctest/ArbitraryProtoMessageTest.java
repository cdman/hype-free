package com.example.protoctest;

import java.util.ArrayList;
import org.junit.*;
import static org.junit.Assert.*;

import com.example.protoctest.AddressBookProtos.Person.PhoneType;
import com.example.protoctest.MetadataProtos.Metadata;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ByteString;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

public final class ArbitraryProtoMessageTest {
	@Test
	public void testArbitraryProtoMessage() throws Exception {
		Message msg = getMessage();
		Metadata metadata = serializeMetadata(msg);
		
		byte[] msgByte = msg.toByteArray();
		metadata = Metadata.newBuilder()
			.mergeFrom( metadata.toByteArray() ).build();
		FileDescriptor fd = deserializeMetadata(metadata);
		
		DynamicMessage dmsg = DynamicMessage.parseFrom(
			fd.getMessageTypes().get(metadata.getFieldDescriptorIdx()), 
			msgByte);
		assertTrue(dmsg.toString().contains("263-1655-2187"));
	}
	
	Metadata serializeMetadata(Message msg) {
		FileDescriptor fd = msg.getDescriptorForType().getFile();
		
		String msgTypeName = msg.getDescriptorForType().getFullName();
		int idx = 0;
		for (Descriptor desc : fd.getMessageTypes()) {
			if (desc.getFullName().equals(  msgTypeName )) { break; }
			++idx;
		}
		
		assert idx < fd.getMessageTypes().size();
		
		return serialize(fd, idx);
	}
	
	Metadata serialize(FileDescriptor fd, int fdIndex) {
		Metadata.Builder result = Metadata.newBuilder();

		for (FileDescriptor dependency : fd.getDependencies()) {
			result.addDependencies(serialize(dependency, -1));
		}
		result.setFieldDescriptor( ByteString.copyFrom( fd.toProto().toByteArray() ) );
		if (fdIndex >= 0) { result.setFieldDescriptorIdx(fdIndex); }
		return result.build();
	}
	
	FileDescriptor deserializeMetadata(Metadata s) throws Exception {
		ArrayList<FileDescriptor> deps = new ArrayList<FileDescriptor>();

		for (Metadata m : s.getDependenciesList()) {
			deps.add( deserializeMetadata(m) );
		}

		FileDescriptorProto fileDescriptorProto = FileDescriptorProto.newBuilder()
			.mergeFrom(s.getFieldDescriptor()).build();
		FileDescriptor fileDescriptor = FileDescriptor.buildFrom(fileDescriptorProto, 
			deps.toArray(new FileDescriptor[deps.size()]));
		return fileDescriptor;
	}
	
	private Message getMessage() {
		return AddressBookProtos.AddressBook.newBuilder().addPerson(
				AddressBookProtos.Person.newBuilder() 
					.setId(42)
					.setName("Foo")
					.setEmail("test@example.com")
					.addPhone(AddressBookProtos.Person.PhoneNumber.newBuilder()
							.setNumber("263-1655-2187")
							.setType(PhoneType.HOME)
					)
			).build();
	}
}
